package com.diabeto.data.model

import com.google.firebase.Timestamp

/**
 * Profil utilisateur stocké dans Firestore (/users/{uid})
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nom: String = "",
    val prenom: String = "",
    val role: UserRole = UserRole.PATIENT,
    val telephone: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    // Donnees morphometriques (PATIENT uniquement — synchro avec Room PatientEntity)
    val poids: Double? = null,
    val taille: Double? = null,
    val tourDeTaille: Double? = null,
    val masseGrasse: Double? = null,
    // Donnees professionnelles (MEDECIN uniquement)
    val specialite: String = "",
    val numeroOrdre: String = "",
    val structureSante: String = "",
    val anneesExperience: Int? = null,
    val modeConsultation: String = "", // TELECONSULTATION | CABINET | LES_DEUX
    val disponibilite: String = "",    // EN_LIGNE | INDISPONIBLE | SUR_RDV
    val joursGarde: String = "",       // ex: "Lun-Ven 8h-18h"
    val languesParlees: String = ""    // ex: "Francais, Anglais"
) {
    val nomComplet: String get() = "$prenom $nom".trim()

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "uid" to uid,
            "email" to email,
            "nom" to nom,
            "prenom" to prenom,
            "role" to role.name,
            "telephone" to telephone,
            "createdAt" to createdAt
        )
        // Patient
        poids?.let { map["poids"] = it }
        taille?.let { map["taille"] = it }
        tourDeTaille?.let { map["tourDeTaille"] = it }
        masseGrasse?.let { map["masseGrasse"] = it }
        // Medecin
        if (specialite.isNotBlank()) map["specialite"] = specialite
        if (numeroOrdre.isNotBlank()) map["numeroOrdre"] = numeroOrdre
        if (structureSante.isNotBlank()) map["structureSante"] = structureSante
        anneesExperience?.let { map["anneesExperience"] = it }
        if (modeConsultation.isNotBlank()) map["modeConsultation"] = modeConsultation
        if (disponibilite.isNotBlank()) map["disponibilite"] = disponibilite
        if (joursGarde.isNotBlank()) map["joursGarde"] = joursGarde
        if (languesParlees.isNotBlank()) map["languesParlees"] = languesParlees
        return map
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile = UserProfile(
            uid = map["uid"] as? String ?: "",
            email = map["email"] as? String ?: "",
            nom = map["nom"] as? String ?: "",
            prenom = map["prenom"] as? String ?: "",
            role = try {
                UserRole.valueOf(map["role"] as? String ?: "PATIENT")
            } catch (e: Exception) {
                UserRole.PATIENT
            },
            telephone = map["telephone"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            poids = (map["poids"] as? Number)?.toDouble(),
            taille = (map["taille"] as? Number)?.toDouble(),
            tourDeTaille = (map["tourDeTaille"] as? Number)?.toDouble(),
            masseGrasse = (map["masseGrasse"] as? Number)?.toDouble(),
            specialite = map["specialite"] as? String ?: "",
            numeroOrdre = map["numeroOrdre"] as? String ?: "",
            structureSante = map["structureSante"] as? String ?: "",
            anneesExperience = (map["anneesExperience"] as? Number)?.toInt(),
            modeConsultation = map["modeConsultation"] as? String ?: "",
            disponibilite = map["disponibilite"] as? String ?: "",
            joursGarde = map["joursGarde"] as? String ?: "",
            languesParlees = map["languesParlees"] as? String ?: ""
        )
    }
}

enum class UserRole {
    PATIENT, MEDECIN
}
