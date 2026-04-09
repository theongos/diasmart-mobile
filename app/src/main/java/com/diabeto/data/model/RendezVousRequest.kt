package com.diabeto.data.model

import com.google.firebase.Timestamp

/**
 * Demande de rendez-vous initiee par un patient.
 * Collection Firestore : /rdv_requests/{id}
 *
 * Cycle de vie :
 *  - PATIENT cree la demande (status = PENDING)
 *  - MEDECIN voit les demandes ou medecinUid == son uid
 *  - MEDECIN accepte (status = ACCEPTED) ou refuse (status = REJECTED)
 *  - Sur acceptation, un RDV est cree dans rdv_shared/{patientUid}/rendezvous
 */
data class RendezVousRequest(
    val id: String = "",
    val patientUid: String = "",
    val patientNom: String = "",
    val medecinUid: String = "",
    val medecinNom: String = "",
    val dateHeureSouhaitee: String = "",   // ISO LocalDateTime, ex "2026-04-15T10:30:00"
    val dureeMinutes: Int = 30,
    val motif: String = "",
    val type: String = "CONSULTATION",
    val status: AppointmentRequestStatus = AppointmentRequestStatus.PENDING,
    val medecinReponse: String = "",        // commentaire du medecin
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "patientUid" to patientUid,
        "patientNom" to patientNom,
        "medecinUid" to medecinUid,
        "medecinNom" to medecinNom,
        "dateHeureSouhaitee" to dateHeureSouhaitee,
        "dureeMinutes" to dureeMinutes,
        "motif" to motif,
        "type" to type,
        "status" to status.name,
        "medecinReponse" to medecinReponse,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): RendezVousRequest = RendezVousRequest(
            id = id,
            patientUid = map["patientUid"] as? String ?: "",
            patientNom = map["patientNom"] as? String ?: "",
            medecinUid = map["medecinUid"] as? String ?: "",
            medecinNom = map["medecinNom"] as? String ?: "",
            dateHeureSouhaitee = map["dateHeureSouhaitee"] as? String ?: "",
            dureeMinutes = (map["dureeMinutes"] as? Number)?.toInt() ?: 30,
            motif = map["motif"] as? String ?: "",
            type = map["type"] as? String ?: "CONSULTATION",
            status = try {
                AppointmentRequestStatus.valueOf(
                    (map["status"] as? String ?: "PENDING").uppercase()
                )
            } catch (e: Exception) {
                AppointmentRequestStatus.PENDING
            },
            medecinReponse = map["medecinReponse"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp
        )
    }
}

enum class AppointmentRequestStatus {
    PENDING, ACCEPTED, REJECTED
}
