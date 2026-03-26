package com.diabeto.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.diabeto.data.entity.HbA1cEntity
import com.diabeto.data.entity.LectureGlucoseEntity
import com.diabeto.data.entity.PatientEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatbotRepository"

/**
 * Repository pour les interactions avec Gemini AI (ROLLY)
 * Prompts durcis : anti-hallucination, ton professionnel, périmètre diabète strict
 */
@Singleton
class ChatbotRepository @Inject constructor(
    private val geminiModel: GenerativeModel
) {
    private var chatSession = geminiModel.startChat()

    // ─────────────────────────────────────────────────────────────────────────
    // CONVERSATION LIBRE
    // ─────────────────────────────────────────────────────────────────────────

    fun envoyerMessage(message: String): Flow<String> = flow {
        try {
            Log.d(TAG, "envoyerMessage: $message")
            val response = chatSession.sendMessage(message)
            Log.d(TAG, "Réponse reçue: ${response.text?.take(100)}")
            emit(response.text ?: "Je n'ai pas pu générer de réponse.")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur envoyerMessage", e)
            emit("❌ Erreur IA : ${e.message}")
        }
    }

    fun envoyerMessageAvecContexte(
        message: String,
        patient: PatientEntity?,
        lecturesRecentes: List<LectureGlucoseEntity>,
        latestHbA1c: HbA1cEntity? = null,
        hba1cEstimee: Double? = null,
        historiqueChat: String = ""
    ): Flow<String> = flow {
        try {
            val contexte = buildContexte(patient, lecturesRecentes, latestHbA1c, hba1cEstimee)
            val sb = StringBuilder()
            if (historiqueChat.isNotBlank()) {
                sb.appendLine(historiqueChat)
                sb.appendLine()
            }
            if (contexte.isNotBlank()) {
                sb.appendLine("Données patient :")
                sb.appendLine(contexte)
                sb.appendLine()
            }
            sb.appendLine("Question : $message")
            val messageComplet = sb.toString().trim()
            Log.d(TAG, "envoyerMessageAvecContexte: ${messageComplet.take(300)}")
            val response = chatSession.sendMessage(messageComplet)
            Log.d(TAG, "Réponse contexte reçue: ${response.text?.take(100)}")
            emit(response.text ?: "Je n'ai pas pu générer de réponse.")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur envoyerMessageAvecContexte", e)
            emit("❌ Erreur IA : ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE DE REPAS → JSON STRUCTURÉ
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun analyserRepasJson(
        descriptionRepas: String,
        patient: PatientEntity? = null
    ): String {
        val contextePatient = patient?.let {
            "Patient : ${it.nomComplet}, ${it.age} ans, Diabète ${it.typeDiabete.name.replace("_", " ")}"
        } ?: ""

        val prompt = """
            $contextePatient

            Analyse nutritionnelle du repas suivant pour un patient diabétique :
            "$descriptionRepas"

            CONSIGNES STRICTES :
            - Réponds UNIQUEMENT avec un objet JSON valide. Aucun texte avant ni après. Pas de balises markdown.
            - Toutes les valeurs nutritionnelles sont des ESTIMATIONS basées sur des portions standards. Ne les présente pas comme exactes.
            - Si un aliment est ambigu ou non identifiable, utilise la variante la plus courante et indique-le dans "description".

            Schéma JSON OBLIGATOIRE :
            {
              "nom_repas": "Nom court du repas",
              "description": "Ingrédients identifiés et hypothèses de portions",
              "glucides_estimes": 45.0,
              "index_glycemique": 55,
              "charge_glycemique": 24.8,
              "calories_estimees": 450,
              "proteines_estimees": 25.0,
              "lipides_estimes": 12.0,
              "fibres_estimees": 6.0,
              "categorie_ig": "moyen",
              "impact_glycemique": "Impact attendu sur la glycémie post-prandiale",
              "recommandations": ["conseil 1", "conseil 2"],
              "alternatives_saines": ["alternative 1", "alternative 2"],
              "score_diabete": 65
            }

            Règles de calcul :
            - glucides_estimes : grammes (décimal)
            - index_glycemique : 0-100 (entier), basé sur les tables IG reconnues
            - charge_glycemique : glucides × IG / 100 (décimal)
            - categorie_ig : "bas" (≤55), "moyen" (56-69), "eleve" (≥70)
            - score_diabete : 0 = très défavorable, 100 = excellent pour un diabétique
            - recommandations : 2-3 conseils concrets et actionnables
            - alternatives_saines : 1-2 substitutions à IG plus bas
        """.trimIndent()

        return try {
            val response = geminiModel.generateContent(prompt)
            response.text ?: throw Exception("Réponse vide de Gemini")
        } catch (e: Exception) {
            throw Exception("Erreur ROLLY lors de l'analyse du repas : ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE GLYCÉMIQUE COMPLÈTE
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun analyserGlycemie(
        patient: PatientEntity,
        lectures: List<LectureGlucoseEntity>,
        latestHbA1c: HbA1cEntity? = null,
        hba1cEstimee: Double? = null
    ): String {
        if (lectures.isEmpty()) return "Aucune lecture de glycémie disponible pour l'analyse."

        val contexte = buildContexte(patient, lectures, latestHbA1c, hba1cEstimee)
        val prompt = """
            $contexte

            ANALYSE GLYCÉMIQUE — Réponds de manière concise et structurée.

            1. **Contrôle glycémique** : Temps dans la cible (70-180 mg/dL), variabilité, moyenne vs objectif.
            2. **Tendances** : Patterns hypo/hyperglycémiques identifiés (heures, contextes). Base-toi UNIQUEMENT sur les données ci-dessus.
            3. **Corrélation HbA1c** : Si HbA1c disponible, compare avec la glycémie moyenne observée. Cohérence ? Écart ?
            4. **Recommandations** : 3-4 actions concrètes et mesurables pour améliorer le contrôle.
            5. **Alertes** : Risques immédiats identifiés dans les données.

            IMPORTANT :
            - N'invente AUCUNE donnée. Analyse UNIQUEMENT ce qui est fourni.
            - Si les données sont insuffisantes pour un point, indique-le.
            - Ton professionnel. Maximum 250 mots.
            - Termine par : "Avis informatif — consultez votre médecin."
        """.trimIndent()

        return try {
            val response = geminiModel.generateContent(prompt)
            response.text ?: "Analyse indisponible."
        } catch (e: Exception) {
            "Erreur lors de l'analyse : ${e.message}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSEILS NUTRITIONNELS
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun conseilsNutritionnels(
        patient: PatientEntity,
        derniereLecture: LectureGlucoseEntity?,
        latestHbA1c: HbA1cEntity? = null
    ): String {
        val typeD = patient.typeDiabete.name.replace("_", " ")
        val glycemie = derniereLecture?.let {
            "Dernière glycémie : ${it.valeur.toInt()} mg/dL (${it.contexte.getDisplayName()})"
        } ?: "Pas de lecture récente"
        val hba1cInfo = latestHbA1c?.let {
            "Dernière HbA1c : ${it.valeur}% (${it.getInterpretation().name.replace("_", " ").lowercase()})"
        } ?: ""

        val prompt = """
            Patient : ${patient.nomComplet}, Diabète $typeD, ${patient.age} ans
            $glycemie
            $hba1cInfo

            CONSEILS NUTRITIONNELS — Concis et actionnables.

            1. **Aliments recommandés** : 5-6 aliments à IG bas, adaptés au diabète $typeD
            2. **Aliments à limiter** : 4-5 aliments à éviter ou réduire, avec alternatives
            3. **Exemple de repas** : 1 journée type (petit-déjeuner, déjeuner, dîner) adaptée
            4. **Collations** : 2-3 options pour prévenir l'hypoglycémie
            5. **Hydratation** : recommandations pratiques

            RÈGLES :
            - Conseils basés sur les recommandations ADA/HAS pour le diabète $typeD.
            - Si la glycémie actuelle est hors cible, adapte les conseils en conséquence.
            - Ne recommande AUCUN complément alimentaire ou produit commercial spécifique.
            - Maximum 200 mots. Ton professionnel.
            - Termine par : "Avis informatif — consultez votre médecin/diététicien."
        """.trimIndent()

        return try {
            val response = geminiModel.generateContent(prompt)
            response.text ?: "Conseils indisponibles."
        } catch (e: Exception) {
            "Erreur : ${e.message}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRÉDICTION DE RISQUE
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun previsionRisque(
        patient: PatientEntity,
        lectures: List<LectureGlucoseEntity>,
        latestHbA1c: HbA1cEntity? = null
    ): String {
        if (lectures.size < 3) return "Minimum 3 lectures nécessaires pour une prévision fiable."

        val contexte = buildContexte(patient, lectures, latestHbA1c, null)
        val prompt = """
            $contexte

            PRÉVISION DE RISQUE GLYCÉMIQUE — Analyse basée STRICTEMENT sur les données ci-dessus.

            1. **Risque hypoglycémie** (prochaines 6h) : Faible / Modéré / Élevé
               - Justification basée sur les tendances observées
            2. **Risque hyperglycémie** (prochaines 6h) : Faible / Modéré / Élevé
               - Justification basée sur les tendances observées
            3. **Signaux d'alerte** : Quels symptômes surveiller
            4. **Actions préventives** : 2-3 mesures concrètes
            5. **Consultation urgente** : Dans quels cas appeler le 15/SAMU

            RÈGLES STRICTES :
            - Base-toi UNIQUEMENT sur les tendances, contextes (à jeun, post-prandial, etc.) et heures des lectures.
            - N'invente AUCUN pattern non visible dans les données.
            - Si les données ne permettent pas une prévision fiable, dis-le clairement.
            - Précise que cette prévision est INDICATIVE et ne remplace pas un suivi médical.
            - Maximum 200 mots. Ton professionnel.
        """.trimIndent()

        return try {
            val response = geminiModel.generateContent(prompt)
            response.text ?: "Prévision indisponible."
        } catch (e: Exception) {
            "Erreur : ${e.message}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISION — RECONNAISSANCE D'IMAGE DE REPAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse une image de repas via Gemini Vision.
     * Envoie le Bitmap avec un prompt nutritionnel et retourne un JSON structuré.
     */
    suspend fun analyserRepasImage(
        bitmap: Bitmap,
        patient: PatientEntity? = null
    ): String {
        val contextePatient = patient?.let {
            "Patient : ${it.nomComplet}, ${it.age} ans, Diabète ${it.typeDiabete.name.replace("_", " ")}"
        } ?: ""

        val prompt = """
            $contextePatient

            Tu es un expert en nutrition spécialisé dans le diabète.
            Analyse cette image de repas. Identifie TOUS les aliments visibles, estime les portions et la charge en glucides.

            CONSIGNES STRICTES :
            - Réponds UNIQUEMENT avec un objet JSON valide. Aucun texte avant ni après. Pas de balises markdown.
            - Identifie chaque aliment visible dans l'image et estime les portions.
            - Si un aliment n'est pas clairement identifiable, fais une estimation raisonnable et indique-le.
            - Toutes les valeurs sont des ESTIMATIONS basées sur l'apparence visuelle.

            Schéma JSON OBLIGATOIRE :
            {
              "nom_repas": "Nom descriptif du repas identifié",
              "description": "Aliments identifiés dans l'image avec portions estimées",
              "glucides_estimes": 45.0,
              "index_glycemique": 55,
              "charge_glycemique": 24.8,
              "calories_estimees": 450,
              "proteines_estimees": 25.0,
              "lipides_estimes": 12.0,
              "fibres_estimees": 6.0,
              "categorie_ig": "moyen",
              "impact_glycemique": "Impact attendu sur la glycémie post-prandiale",
              "recommandations": ["conseil 1", "conseil 2"],
              "alternatives_saines": ["alternative 1", "alternative 2"],
              "score_diabete": 65
            }

            Règles de calcul :
            - glucides_estimes : grammes (décimal)
            - index_glycemique : 0-100 (entier), basé sur les tables IG reconnues
            - charge_glycemique : glucides × IG / 100 (décimal)
            - categorie_ig : "bas" (≤55), "moyen" (56-69), "eleve" (≥70)
            - score_diabete : 0 = très défavorable, 100 = excellent pour un diabétique
            - recommandations : 2-3 conseils concrets et actionnables
            - alternatives_saines : 1-2 substitutions à IG plus bas
        """.trimIndent()

        return try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = geminiModel.generateContent(inputContent)
            response.text ?: throw Exception("Réponse vide de Gemini Vision")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Vision repas", e)
            throw Exception("Erreur ROLLY Vision : ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE PRÉDICTIVE — TENDANCES GLYCÉMIQUES 7 JOURS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse prédictive complète basée sur l'historique glycémique de 7 jours.
     * Détecte les patterns, prédit les risques et propose des recommandations.
     */
    suspend fun analysePredictive7Jours(
        patient: PatientEntity,
        lectures: List<LectureGlucoseEntity>,
        latestHbA1c: HbA1cEntity? = null,
        hba1cEstimee: Double? = null
    ): String {
        if (lectures.isEmpty()) return "Aucune donnée glycémique disponible pour l'analyse prédictive."
        if (lectures.size < 5) return "Minimum 5 lectures nécessaires pour une analyse prédictive fiable. Actuellement : ${lectures.size} lectures."

        val contexte = buildContexte(patient, lectures, latestHbA1c, hba1cEstimee)
        val prompt = """
            $contexte

            ANALYSE PRÉDICTIVE DES TENDANCES GLYCÉMIQUES — 7 DERNIERS JOURS

            Tu es un expert en diabétologie clinique. Analyse les données ci-dessus avec rigueur.
            Base-toi UNIQUEMENT sur les données fournies. N'invente aucune valeur.

            Structure ta réponse ainsi :

            ## 📊 Résumé statistique
            - Glycémie moyenne, min, max
            - Temps dans la cible (TIR : 70-180 mg/dL)
            - Écart-type / variabilité glycémique
            - Coefficient de variation (CV = écart-type/moyenne × 100)

            ## 📈 Tendances identifiées
            - Patterns récurrents (horaires, contextes)
            - Tendance globale : amélioration / stable / dégradation
            - Phénomène de l'aube détecté ? (glycémie élevée 4h-8h)
            - Réponse post-prandiale : normale / exagérée / retardée

            ## ⚠️ Risques prédictifs (prochaines 24-48h)
            - Risque hypoglycémie : Faible / Modéré / Élevé — justification
            - Risque hyperglycémie : Faible / Modéré / Élevé — justification
            - Situations à risque identifiées

            ## 🎯 Recommandations préventives
            - 4-5 actions concrètes, mesurables et personnalisées
            - Adaptées au type de diabète et au profil du patient
            - Classées par priorité

            ## 🔮 Projection
            - Si les tendances actuelles se maintiennent, quel impact sur l'HbA1c estimée ?
            - Points d'attention pour les prochains jours

            RÈGLES :
            - N'invente AUCUNE donnée. Analyse UNIQUEMENT ce qui est fourni.
            - Si données insuffisantes pour un point, indique-le.
            - Ton professionnel, concis et structuré.
            - Termine par : "Avis informatif — consultez votre médecin."
        """.trimIndent()

        return try {
            val response = geminiModel.generateContent(prompt)
            response.text ?: "Analyse prédictive indisponible."
        } catch (e: Exception) {
            Log.e(TAG, "Erreur analyse prédictive", e)
            "Erreur lors de l'analyse prédictive : ${e.message}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    fun reinitialiserChat() {
        chatSession = geminiModel.startChat()
    }

    private fun buildContexte(
        patient: PatientEntity?,
        lectures: List<LectureGlucoseEntity>,
        latestHbA1c: HbA1cEntity? = null,
        hba1cEstimee: Double? = null
    ): String {
        if (patient == null && lectures.isEmpty()) return ""
        val sb = StringBuilder()

        patient?.let {
            sb.appendLine("Patient : ${it.nomComplet}, ${it.age} ans, Sexe : ${it.sexe.name}")
            sb.appendLine("Type de diabète : ${it.typeDiabete.name.replace("_", " ")}")
            // Données corporelles
            val metrics = mutableListOf<String>()
            it.poids?.let { p -> metrics.add("Poids : ${p}kg") }
            it.taille?.let { t -> metrics.add("Taille : ${t}cm") }
            it.imc?.let { imc -> metrics.add("IMC : ${"%.1f".format(imc)} kg/m² (${it.categorieImc})") }
            it.tourDeTaille?.let { tdt -> metrics.add("Tour de taille : ${tdt}cm (${it.risqueTourDeTaille})") }
            it.masseGrasse?.let { mg -> metrics.add("Masse grasse : ${mg}%") }
            if (metrics.isNotEmpty()) {
                sb.appendLine("Données corporelles : ${metrics.joinToString(" | ")}")
            }
        }

        // HbA1c
        latestHbA1c?.let {
            val source = if (it.estEstimation) "estimée" else "labo${if (it.laboratoire.isNotBlank()) " (${it.laboratoire})" else ""}"
            sb.appendLine("Dernière HbA1c : ${it.valeur}% ($source) — ${it.dateMesure}")
            sb.appendLine("  → Interprétation : ${it.getInterpretation().name.replace("_", " ")}")
            sb.appendLine("  → Glycémie moyenne estimée (eAG) : ${it.getGlycemieMoyenneEstimee().toInt()} mg/dL")
        }
        hba1cEstimee?.let {
            sb.appendLine("HbA1c estimée (30 derniers jours) : ${it}%")
        }

        // Lectures glycémiques
        if (lectures.isNotEmpty()) {
            val fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            sb.appendLine("Lectures glycémiques récentes (${lectures.size}) :")
            lectures.take(15).forEach { l ->
                sb.appendLine("  - ${l.dateHeure.format(fmt)} : ${l.valeur.toInt()} mg/dL (${l.contexte.getDisplayName()})")
            }
            val moyenne = lectures.map { it.valeur }.average()
            val dansLaCible = lectures.count { it.valeur in 70.0..180.0 }
            val hypos = lectures.count { it.valeur < 70 }
            val hypers = lectures.count { it.valeur > 180 }
            val tir = if (lectures.isNotEmpty()) (dansLaCible * 100 / lectures.size) else 0
            sb.appendLine("Moyenne : ${moyenne.toInt()} mg/dL | TIR : $tir% | Hypos : $hypos | Hypers : $hypers")
        }

        return sb.toString().trim()
    }
}
