package com.diabeto.data.model

import com.google.firebase.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Résultat d'analyse d'un repas par ROLLY (Gemini).
 * Le modèle IA renvoie du JSON structuré qui est parsé en cette data class.
 */
@Serializable
data class RepasAnalyse(
    @SerialName("nom_repas")
    val nomRepas: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("glucides_estimes")
    val glucidesEstimes: Double = 0.0,

    @SerialName("index_glycemique")
    val indexGlycemique: Int = 0,

    @SerialName("charge_glycemique")
    val chargeGlycemique: Double = 0.0,

    @SerialName("calories_estimees")
    val caloriesEstimees: Int = 0,

    @SerialName("proteines_estimees")
    val proteinesEstimees: Double = 0.0,

    @SerialName("lipides_estimes")
    val lipidesEstimes: Double = 0.0,

    @SerialName("fibres_estimees")
    val fibresEstimees: Double = 0.0,

    @SerialName("categorie_ig")
    val categorieIG: String = "moyen", // "bas", "moyen", "eleve"

    @SerialName("impact_glycemique")
    val impactGlycemique: String = "", // Explication de l'impact

    @SerialName("recommandations")
    val recommandations: List<String> = emptyList(),

    @SerialName("alternatives_saines")
    val alternativesSaines: List<String> = emptyList(),

    @SerialName("score_diabete")
    val scoreDiabete: Int = 50 // 0 = très mauvais, 100 = excellent pour diabétique
)

/**
 * Document Firestore représentant un repas enregistré
 * Chemin : /users/{uid}/repas/{repasId}
 */
data class RepasDocument(
    val id: String = "",
    val userId: String = "",
    val nomRepas: String = "",
    val description: String = "",
    val glucidesEstimes: Double = 0.0,
    val indexGlycemique: Int = 0,
    val chargeGlycemique: Double = 0.0,
    val caloriesEstimees: Int = 0,
    val proteinesEstimees: Double = 0.0,
    val lipidesEstimes: Double = 0.0,
    val fibresEstimees: Double = 0.0,
    val categorieIG: String = "moyen",
    val impactGlycemique: String = "",
    val recommandations: List<String> = emptyList(),
    val alternativesSaines: List<String> = emptyList(),
    val scoreDiabete: Int = 50,
    val confirmeParUtilisateur: Boolean = false,
    val glycemieAvantRepas: Double? = null,
    val glycemieApresRepas: Double? = null,
    val timestamp: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "nomRepas" to nomRepas,
        "description" to description,
        "glucidesEstimes" to glucidesEstimes,
        "indexGlycemique" to indexGlycemique,
        "chargeGlycemique" to chargeGlycemique,
        "caloriesEstimees" to caloriesEstimees,
        "proteinesEstimees" to proteinesEstimees,
        "lipidesEstimes" to lipidesEstimes,
        "fibresEstimees" to fibresEstimees,
        "categorieIG" to categorieIG,
        "impactGlycemique" to impactGlycemique,
        "recommandations" to recommandations,
        "alternativesSaines" to alternativesSaines,
        "scoreDiabete" to scoreDiabete,
        "confirmeParUtilisateur" to confirmeParUtilisateur,
        "glycemieAvantRepas" to glycemieAvantRepas,
        "glycemieApresRepas" to glycemieApresRepas,
        "timestamp" to timestamp
    )

    companion object {
        fun fromAnalyse(analyse: RepasAnalyse, userId: String): RepasDocument =
            RepasDocument(
                userId = userId,
                nomRepas = analyse.nomRepas,
                description = analyse.description,
                glucidesEstimes = analyse.glucidesEstimes,
                indexGlycemique = analyse.indexGlycemique,
                chargeGlycemique = analyse.chargeGlycemique,
                caloriesEstimees = analyse.caloriesEstimees,
                proteinesEstimees = analyse.proteinesEstimees,
                lipidesEstimes = analyse.lipidesEstimes,
                fibresEstimees = analyse.fibresEstimees,
                categorieIG = analyse.categorieIG,
                impactGlycemique = analyse.impactGlycemique,
                recommandations = analyse.recommandations,
                alternativesSaines = analyse.alternativesSaines,
                scoreDiabete = analyse.scoreDiabete
            )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): RepasDocument = RepasDocument(
            id = id,
            userId = map["userId"] as? String ?: "",
            nomRepas = map["nomRepas"] as? String ?: "",
            description = map["description"] as? String ?: "",
            glucidesEstimes = (map["glucidesEstimes"] as? Number)?.toDouble() ?: 0.0,
            indexGlycemique = (map["indexGlycemique"] as? Number)?.toInt() ?: 0,
            chargeGlycemique = (map["chargeGlycemique"] as? Number)?.toDouble() ?: 0.0,
            caloriesEstimees = (map["caloriesEstimees"] as? Number)?.toInt() ?: 0,
            proteinesEstimees = (map["proteinesEstimees"] as? Number)?.toDouble() ?: 0.0,
            lipidesEstimes = (map["lipidesEstimes"] as? Number)?.toDouble() ?: 0.0,
            fibresEstimees = (map["fibresEstimees"] as? Number)?.toDouble() ?: 0.0,
            categorieIG = map["categorieIG"] as? String ?: "moyen",
            impactGlycemique = map["impactGlycemique"] as? String ?: "",
            recommandations = map["recommandations"] as? List<String> ?: emptyList(),
            alternativesSaines = map["alternativesSaines"] as? List<String> ?: emptyList(),
            scoreDiabete = (map["scoreDiabete"] as? Number)?.toInt() ?: 50,
            confirmeParUtilisateur = map["confirmeParUtilisateur"] as? Boolean ?: false,
            glycemieAvantRepas = (map["glycemieAvantRepas"] as? Number)?.toDouble(),
            glycemieApresRepas = (map["glycemieApresRepas"] as? Number)?.toDouble(),
            timestamp = map["timestamp"] as? Timestamp ?: Timestamp.now()
        )
    }
}
