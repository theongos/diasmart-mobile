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
    // Donnees morphometriques (synchro Patient <-> Firestore)
    val poids: Double? = null,
    val taille: Double? = null,
    val tourDeTaille: Double? = null,
    val masseGrasse: Double? = null
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
        poids?.let { map["poids"] = it }
        taille?.let { map["taille"] = it }
        tourDeTaille?.let { map["tourDeTaille"] = it }
        masseGrasse?.let { map["masseGrasse"] = it }
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
            masseGrasse = (map["masseGrasse"] as? Number)?.toDouble()
        )
    }
}

enum class UserRole {
    PATIENT, MEDECIN
}
