package com.diabeto.data.model

import com.google.firebase.Timestamp

/**
 * Validation d'une réponse ROLLY par un médecin
 * Collection Firestore: /validations/{id}
 */
data class RollyValidation(
    val id: String = "",
    val patientUid: String = "",
    val patientNom: String = "",
    val medecinUid: String = "",
    val medecinNom: String = "",
    val question: String = "",
    val rollyResponse: String = "",
    val status: ValidationStatus = ValidationStatus.PENDING,
    val medecinComment: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val validatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "patientUid" to patientUid,
        "patientNom" to patientNom,
        "medecinUid" to medecinUid,
        "medecinNom" to medecinNom,
        "question" to question,
        "rollyResponse" to rollyResponse,
        "status" to status.name.lowercase(),
        "medecinComment" to medecinComment,
        "createdAt" to createdAt,
        "validatedAt" to validatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): RollyValidation = RollyValidation(
            id = id,
            patientUid = map["patientUid"] as? String ?: "",
            patientNom = map["patientNom"] as? String ?: "",
            medecinUid = map["medecinUid"] as? String ?: "",
            medecinNom = map["medecinNom"] as? String ?: "",
            question = map["question"] as? String ?: "",
            rollyResponse = map["rollyResponse"] as? String ?: "",
            status = try {
                ValidationStatus.valueOf((map["status"] as? String ?: "pending").uppercase())
            } catch (e: Exception) {
                ValidationStatus.PENDING
            },
            medecinComment = map["medecinComment"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            validatedAt = map["validatedAt"] as? Timestamp
        )
    }
}

enum class ValidationStatus {
    PENDING, VALIDATED, REJECTED
}
