package com.diabeto.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LocalAIManager"
private const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
private const val MODEL_SIZE_MB = 550 // Taille approximative du modele

/**
 * Gestionnaire d'IA locale (on-device) pour DiaSmart.AI
 *
 * Utilise MediaPipe LLM Inference pour executer Gemma 3 1B directement
 * sur l'appareil Android, sans connexion internet.
 *
 * Architecture hybride :
 * - En ligne  -> Gemini 2.5 Flash (cloud, puissant)
 * - Hors ligne -> Gemma 3 1B (local, leger, basique)
 */
@Singleton
class LocalAIManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var llmInference: LlmInference? = null
    private var isInitialized = false
    private var initError: String? = null

    // Prompt systeme ULTRA-COMPACT pour vitesse maximale (Gemma 3 1B)
    // Chaque token dans le prompt = latence en plus. On reste minimal.
    private val systemPrompt = """Tu es ROLLY, assistant diabete Cameroun. Reponds en francais, court. Pas de diagnostic. Urgence si glycemie<0.70 ou >3.0 g/L."""

    // ─────────────────────────────────────────────────────────────────
    // CONNECTIVITE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Verifie si l'appareil a une connexion internet active.
     */
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ─────────────────────────────────────────────────────────────────
    // GESTION DU MODELE LOCAL
    // ─────────────────────────────────────────────────────────────────

    /**
     * Verifie si le modele Gemma est telecharge localement.
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > 1_000_000 // > 1 Mo
    }

    /**
     * Retourne le chemin du fichier modele.
     */
    private fun getModelPath(): String {
        return File(context.filesDir, MODEL_FILENAME).absolutePath
    }

    /**
     * Retourne la taille estimee du modele en Mo.
     */
    fun getModelSizeMB(): Int = MODEL_SIZE_MB

    /**
     * Retourne le statut d'initialisation du modele local.
     */
    fun getStatus(): LocalAIStatus {
        return when {
            !isModelDownloaded() -> LocalAIStatus.NOT_DOWNLOADED
            !isInitialized && initError != null -> LocalAIStatus.ERROR
            !isInitialized -> LocalAIStatus.DOWNLOADED
            else -> LocalAIStatus.READY
        }
    }

    /**
     * Initialise le modele local Gemma.
     * Appeler une seule fois, idealement au demarrage de l'app.
     */
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && llmInference != null) return@withContext true
        if (!isModelDownloaded()) {
            initError = "Modele non telecharge"
            Log.w(TAG, "Model not downloaded at: ${getModelPath()}")
            return@withContext false
        }

        try {
            Log.d(TAG, "Initializing local Gemma model...")
            // OPTIMISATIONS VITESSE :
            // - setMaxTokens(256) : divise par 2 le temps de generation max
            //   (Gemma genere sequentiellement, moins de tokens = reponse plus rapide)
            // - Reponses plus courtes mais amplement suffisantes pour ROLLY
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(getModelPath())
                .setMaxTokens(256)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            initError = null
            Log.d(TAG, "Local Gemma model initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize local model", e)
            initError = e.message
            isInitialized = false
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TELECHARGEMENT DU MODELE DEPUIS FIREBASE STORAGE
    // ─────────────────────────────────────────────────────────────────

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    /**
     * Telecharge le modele Gemma depuis Firebase Storage.
     * Chemin Firebase : models/gemma3-1b-it-int4.task
     * Destination : context.filesDir/gemma3-1b-it-int4.task
     */
    suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            Log.d(TAG, "Model already downloaded")
            return@withContext true
        }
        if (_isDownloading.value) {
            Log.w(TAG, "Download already in progress")
            return@withContext false
        }

        _isDownloading.value = true
        _downloadProgress.value = 0f
        _downloadError.value = null

        try {
            val storage = FirebaseStorage.getInstance()
            val modelRef = storage.reference.child("models/$MODEL_FILENAME")
            val destFile = File(context.filesDir, MODEL_FILENAME)
            val tempFile = File(context.filesDir, "${MODEL_FILENAME}.tmp")

            Log.d(TAG, "Starting model download from Firebase Storage...")

            // Verifier que le fichier existe sur Firebase
            val metadata = modelRef.metadata.await()
            val totalBytes = metadata.sizeBytes
            Log.d(TAG, "Model size on server: ${totalBytes / 1024 / 1024} MB")

            // Telecharger vers fichier temporaire
            val downloadTask = modelRef.getFile(tempFile)

            // Observer la progression
            downloadTask.addOnProgressListener { snapshot ->
                val progress = if (snapshot.totalByteCount > 0) {
                    snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                } else 0f
                _downloadProgress.value = progress
                if (snapshot.bytesTransferred % (10 * 1024 * 1024) < 1024 * 1024) {
                    Log.d(TAG, "Download progress: ${(progress * 100).toInt()}%")
                }
            }

            downloadTask.await()

            // Renommer le fichier temporaire
            if (tempFile.exists()) {
                tempFile.renameTo(destFile)
                Log.d(TAG, "Model downloaded successfully: ${destFile.length() / 1024 / 1024} MB")
                _downloadProgress.value = 1f
                _isDownloading.value = false
                true
            } else {
                throw Exception("Download completed but temp file not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model", e)
            _downloadError.value = e.message ?: "Erreur de telechargement"
            _isDownloading.value = false
            _downloadProgress.value = 0f
            // Nettoyer le fichier temporaire
            File(context.filesDir, "${MODEL_FILENAME}.tmp").delete()
            false
        }
    }

    /**
     * Supprime le modele local pour liberer l'espace.
     */
    fun deleteModel() {
        release()
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        if (modelFile.exists()) {
            modelFile.delete()
            Log.d(TAG, "Local model deleted")
        }
    }

    /**
     * Libere les ressources du modele local.
     */
    fun release() {
        try {
            llmInference?.close()
            llmInference = null
            isInitialized = false
            Log.d(TAG, "Local model released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing local model", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GENERATION DE REPONSES (HORS LIGNE)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Genere une reponse locale (non-streaming).
     */
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            if (!initializeModel()) {
                return@withContext "Mode hors-ligne indisponible. Le modele IA local n'est pas installe. " +
                        "Connectez-vous a internet pour utiliser ROLLY."
            }
        }

        try {
            // Format ultra-compact : moins de tokens en entree
            val fullPrompt = "$systemPrompt\nQ: $prompt\nROLLY:"
            val response = llmInference?.generateResponse(fullPrompt)
            response?.takeIf { it.isNotBlank() }
                ?: "Je n'ai pas pu generer de reponse en mode hors-ligne."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating local response", e)
            "Erreur du modele local : ${e.message}"
        }
    }

    /**
     * Genere une reponse locale avec contexte patient.
     */
    suspend fun generateResponseWithContext(
        message: String,
        patientContext: String = "",
        historiqueChat: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            if (!initializeModel()) {
                return@withContext "Mode hors-ligne indisponible. Connectez-vous a internet."
            }
        }

        try {
            // OPTIMISATIONS VITESSE : moins de tokens en entree = prefill plus rapide
            val sb = StringBuilder()
            sb.appendLine(systemPrompt)
            sb.appendLine()
            if (patientContext.isNotBlank()) {
                sb.appendLine("Patient:")
                sb.appendLine(patientContext.take(150)) // Reduit de 300 -> 150 chars
                sb.appendLine()
            }
            if (historiqueChat.isNotBlank()) {
                // Reduit de 6 -> 4 lignes pour accelerer le prefill
                val recentHistory = historiqueChat.lines().takeLast(4).joinToString("\n")
                sb.appendLine("Hist:")
                sb.appendLine(recentHistory)
                sb.appendLine()
            }
            sb.appendLine("Q: $message")
            sb.appendLine("ROLLY:")

            val response = llmInference?.generateResponse(sb.toString())
            response?.takeIf { it.isNotBlank() }
                ?: "Je n'ai pas pu generer de reponse en mode hors-ligne."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating local response with context", e)
            "Erreur du modele local : ${e.message}"
        }
    }

    /**
     * Genere une reponse locale en VRAI streaming (MediaPipe generateResponseAsync).
     *
     * OPTIMISATION VITESSE PERCUE :
     * Avant : on attendait la reponse complete, puis on simulait le streaming.
     *         -> l'utilisateur voyait "..." pendant 5-15s
     * Apres : chaque token est affiche des qu'il est genere par le modele.
     *         -> l'utilisateur voit les mots apparaitre immediatement (UX x10)
     */
    fun generateResponseStream(prompt: String): Flow<String> = channelFlow {
        if (!isInitialized || llmInference == null) {
            if (!initializeModel()) {
                send("Mode hors-ligne indisponible. Connectez-vous a internet.")
                return@channelFlow
            }
        }

        try {
            val fullPrompt = "$systemPrompt\nQ: $prompt\nROLLY:"
            val accumulated = StringBuilder()
            val completed = CompletableDeferred<Unit>()

            // MediaPipe streaming natif : callback appele avec chaque token
            llmInference?.generateResponseAsync(fullPrompt) { partialResult, done ->
                if (partialResult != null) {
                    accumulated.append(partialResult)
                    trySend(accumulated.toString())
                }
                if (done) {
                    if (accumulated.isEmpty()) {
                        trySend("Je n'ai pas pu generer de reponse en mode hors-ligne.")
                    }
                    completed.complete(Unit)
                }
            }

            completed.await() // Attend la fin de la generation
        } catch (e: Exception) {
            Log.e(TAG, "Error in local stream", e)
            send("Erreur du modele local : ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Analyse basique de glycemie en mode hors-ligne.
     * Utilise des regles codees en dur + modele local pour les conseils.
     */
    suspend fun analyserGlycemieLocale(glycemie: Double, moment: String = ""): String {
        val interpretation = when {
            glycemie < 0.54 -> "URGENCE HYPOGLYCEMIE SEVERE"
            glycemie < 0.70 -> "Hypoglycemie - prenez du sucre rapidement"
            glycemie in 0.70..1.10 -> "Glycemie normale a jeun"
            glycemie in 1.10..1.26 -> "Pre-diabete - surveillance recommandee"
            glycemie in 1.26..1.80 -> "Hyperglycemie moderee"
            glycemie in 1.80..2.50 -> "Hyperglycemie elevee - consultez votre medecin"
            glycemie > 2.50 -> "URGENCE HYPERGLYCEMIE SEVERE"
            else -> "Valeur non reconnue"
        }

        val baseResponse = """
            |Analyse glycemie (mode hors-ligne) :
            |Valeur : ${glycemie} g/L ${if (moment.isNotBlank()) "($moment)" else ""}
            |Interpretation : $interpretation
        """.trimMargin()

        // Si le modele local est dispo, enrichir avec des conseils
        return if (isInitialized && llmInference != null) {
            try {
                val conseil = generateResponse(
                    "Ma glycemie est de $glycemie g/L. $interpretation. Donne 2-3 conseils pratiques courts."
                )
                "$baseResponse\n\nConseils ROLLY :\n$conseil"
            } catch (e: Exception) {
                baseResponse
            }
        } else {
            "$baseResponse\n\nConseil : Consultez votre medecin pour un suivi personnalise."
        }
    }
}

/**
 * Statut du modele IA local.
 */
enum class LocalAIStatus {
    NOT_DOWNLOADED,  // Modele pas encore telecharge
    DOWNLOADED,      // Telecharge mais pas charge en memoire
    READY,           // Charge et pret a utiliser
    ERROR            // Erreur d'initialisation
}
