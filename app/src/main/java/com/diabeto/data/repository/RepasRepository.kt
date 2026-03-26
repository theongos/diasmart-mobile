package com.diabeto.data.repository

import com.diabeto.data.model.RepasAnalyse
import com.diabeto.data.model.RepasDocument
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des repas analysés par ROLLY.
 *
 * Structure Firestore :
 *   /users/{uid}/repas/{repasId}
 *
 * Chaque document contient les valeurs nutritionnelles estimées par l'IA
 * (glucidesEstimes, indexGlycemique, etc.) confirmées par l'utilisateur.
 */
@Singleton
class RepasRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val COL_USERS = "users"
        private const val COL_REPAS = "repas"
    }

    // JSON parser configuré pour être tolérant
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsing JSON → RepasAnalyse
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse la réponse JSON de Gemini en RepasAnalyse.
     * Gère les erreurs de parsing (réseau, contenu inapproprié, JSON malformé).
     *
     * @param jsonString la réponse brute de Gemini
     * @return Result<RepasAnalyse> - Success si le parsing réussit, Failure sinon
     */
    fun parseAnalyseJson(jsonString: String): Result<RepasAnalyse> {
        return try {
            // Nettoyer la réponse : Gemini peut entourer le JSON de ```json ... ```
            val cleaned = cleanJsonResponse(jsonString)
            val analyse = json.decodeFromString<RepasAnalyse>(cleaned)

            // Validation minimale
            if (analyse.nomRepas.isBlank()) {
                Result.failure(Exception("Le nom du repas est vide — l'IA n'a pas pu analyser ce repas."))
            } else {
                Result.success(analyse)
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Result.failure(
                Exception("Erreur de parsing JSON : le format de la réponse IA est invalide.\n" +
                        "Détail : ${e.message}\n\nVeuillez réessayer.")
            )
        } catch (e: IllegalArgumentException) {
            Result.failure(
                Exception("Réponse IA invalide : ${e.message}\nVeuillez reformuler votre demande.")
            )
        } catch (e: Exception) {
            Result.failure(
                Exception("Erreur inattendue lors du parsing : ${e.message}")
            )
        }
    }

    /**
     * Nettoie la réponse de Gemini qui peut contenir des balises markdown
     */
    private fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()
        // Supprimer les balises ```json ... ``` courantes
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trimStart()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trimStart()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trimEnd()
        }
        return cleaned
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sauvegarde Firestore
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sauvegarde l'analyse d'un repas dans Firestore après confirmation
     * de l'utilisateur (validation médicale).
     *
     * Chemin : /users/{uid}/repas/{auto-id}
     *
     * @param analyse le résultat analysé par ROLLY
     * @param glycemieAvant glycémie mesurée avant le repas (optionnel)
     * @param glycemieApres glycémie mesurée après le repas (optionnel)
     * @return Result<String> - l'ID du document créé
     */
    suspend fun saveToFirestore(
        analyse: RepasAnalyse,
        glycemieAvant: Double? = null,
        glycemieApres: Double? = null
    ): Result<String> {
        val uid = authRepository.currentUserId
            ?: return Result.failure(Exception("Utilisateur non connecté"))

        return try {
            val document = RepasDocument.fromAnalyse(analyse, uid).copy(
                confirmeParUtilisateur = true,
                glycemieAvantRepas = glycemieAvant,
                glycemieApresRepas = glycemieApres,
                timestamp = Timestamp.now()
            )

            val docRef = firestore.collection(COL_USERS)
                .document(uid)
                .collection(COL_REPAS)
                .add(document.toMap())
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(Exception("Erreur lors de l'enregistrement : ${e.message}"))
        }
    }

    /**
     * Sauvegarde avec possibilité de modifier les valeurs estimées
     * (l'utilisateur peut corriger glucides, IG, etc. avant de confirmer)
     */
    suspend fun saveToFirestoreAvecCorrections(
        document: RepasDocument
    ): Result<String> {
        val uid = authRepository.currentUserId
            ?: return Result.failure(Exception("Utilisateur non connecté"))

        return try {
            val finalDoc = document.copy(
                userId = uid,
                confirmeParUtilisateur = true,
                timestamp = Timestamp.now()
            )

            val docRef = firestore.collection(COL_USERS)
                .document(uid)
                .collection(COL_REPAS)
                .add(finalDoc.toMap())
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(Exception("Erreur : ${e.message}"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Historique des repas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Flow en temps réel de l'historique des repas de l'utilisateur courant
     * (pour les graphiques de suivi)
     */
    fun getRepasHistoriqueFlow(): Flow<List<RepasDocument>> = callbackFlow {
        val uid = authRepository.currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COL_USERS)
            .document(uid)
            .collection(COL_REPAS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val repas = snap?.documents?.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    doc.data?.let { RepasDocument.fromMap(doc.id, it as Map<String, Any?>) }
                } ?: emptyList()
                trySend(repas)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Récupère les repas des N derniers jours (pour statistiques/graphiques)
     */
    suspend fun getRepasDepuis(jours: Int): List<RepasDocument> {
        val uid = authRepository.currentUserId ?: return emptyList()
        val depuis = Timestamp(
            java.util.Date(System.currentTimeMillis() - jours * 24L * 60 * 60 * 1000)
        )

        return try {
            val snap = firestore.collection(COL_USERS)
                .document(uid)
                .collection(COL_REPAS)
                .whereGreaterThan("timestamp", depuis)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                @Suppress("UNCHECKED_CAST")
                doc.data?.let { RepasDocument.fromMap(doc.id, it as Map<String, Any?>) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Supprime un repas de l'historique
     */
    suspend fun supprimerRepas(repasId: String): Result<Unit> {
        val uid = authRepository.currentUserId
            ?: return Result.failure(Exception("Utilisateur non connecté"))

        return try {
            firestore.collection(COL_USERS)
                .document(uid)
                .collection(COL_REPAS)
                .document(repasId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
