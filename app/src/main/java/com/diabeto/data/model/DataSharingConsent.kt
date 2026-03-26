package com.diabeto.data.model

import com.google.firebase.Timestamp

/**
 * Consentement de partage de données entre patient et médecin.
 * Stocké dans Firestore (/data_sharing/{patientUid_medecinUid})
 */
data class DataSharingConsent(
    val patientUid: String = "",
    val medecinUid: String = "",
    val patientNom: String = "",
    val medecinNom: String = "",
    val isActive: Boolean = false,
    val status: ConsentStatus = ConsentStatus.PENDING,
    val grantedAt: Timestamp = Timestamp.now(),
    val revokedAt: Timestamp? = null,
    // Types de données partagées
    val shareGlucose: Boolean = true,
    val shareHbA1c: Boolean = true,
    val shareMedications: Boolean = true,
    val shareBodyMetrics: Boolean = true
) {
    val documentId: String get() = "${patientUid}_${medecinUid}"

    fun toMap(): Map<String, Any?> = mapOf(
        "patientUid" to patientUid,
        "medecinUid" to medecinUid,
        "patientNom" to patientNom,
        "medecinNom" to medecinNom,
        "isActive" to isActive,
        "status" to status.name,
        "grantedAt" to grantedAt,
        "revokedAt" to revokedAt,
        "shareGlucose" to shareGlucose,
        "shareHbA1c" to shareHbA1c,
        "shareMedications" to shareMedications,
        "shareBodyMetrics" to shareBodyMetrics
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): DataSharingConsent = DataSharingConsent(
            patientUid = map["patientUid"] as? String ?: "",
            medecinUid = map["medecinUid"] as? String ?: "",
            patientNom = map["patientNom"] as? String ?: "",
            medecinNom = map["medecinNom"] as? String ?: "",
            isActive = map["isActive"] as? Boolean ?: false,
            status = try {
                ConsentStatus.valueOf(map["status"] as? String ?: "PENDING")
            } catch (_: Exception) {
                if (map["isActive"] as? Boolean == true) ConsentStatus.ACCEPTED else ConsentStatus.PENDING
            },
            grantedAt = map["grantedAt"] as? Timestamp ?: Timestamp.now(),
            revokedAt = map["revokedAt"] as? Timestamp,
            shareGlucose = map["shareGlucose"] as? Boolean ?: true,
            shareHbA1c = map["shareHbA1c"] as? Boolean ?: true,
            shareMedications = map["shareMedications"] as? Boolean ?: true,
            shareBodyMetrics = map["shareBodyMetrics"] as? Boolean ?: true
        )
    }
}

enum class ConsentStatus {
    PENDING,   // Demande envoyée par le médecin, en attente de réponse du patient
    ACCEPTED,  // Patient a accepté
    REJECTED   // Patient a refusé
}
