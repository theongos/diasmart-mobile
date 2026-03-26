package com.diabeto.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entité LectureGlucose pour Room Database
 */
@Entity(
    tableName = "lectures_glucose",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["dateHeure"])
    ]
)
data class LectureGlucoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val valeur: Double, // mg/dL
    val dateHeure: LocalDateTime,
    val contexte: ContexteGlucose,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class ContexteGlucose {
    A_JEUN,
    AVANT_REPAS,
    APRES_REPAS_1H,
    APRES_REPAS_2H,
    AVANT_EXERCICE,
    APRES_EXERCICE,
    AU_LIT,
    REVEIL,
    AUTRE;

    fun getDisplayName(): String = when (this) {
        A_JEUN -> "À jeun"
        AVANT_REPAS -> "Avant repas"
        APRES_REPAS_1H -> "Après repas (1h)"
        APRES_REPAS_2H -> "Après repas (2h)"
        AVANT_EXERCICE -> "Avant exercice"
        APRES_EXERCICE -> "Après exercice"
        AU_LIT -> "Au coucher"
        REVEIL -> "Au réveil"
        AUTRE -> "Autre"
    }
}

/**
 * Statistiques de glycémie calculées
 */
data class GlucoseStatistics(
    val moyenne: Double = 0.0,
    val minimum: Double = 0.0,
    val maximum: Double = 0.0,
    val totalLectures: Int = 0,
    val timeInRange: Double = 0.0, // Pourcentage entre 70-180 mg/dL
    val timeBelowRange: Double = 0.0, // Pourcentage < 70 mg/dL
    val timeAboveRange: Double = 0.0, // Pourcentage > 180 mg/dL
    val periodDays: Int = 0
) {
    companion object {
        const val TARGET_MIN = 70.0
        const val TARGET_MAX = 180.0
        const val SEVERE_HYPO = 54.0
        const val SEVERE_HYPER = 250.0
    }
    
    fun getStatusColor(): String = when {
        timeInRange >= 70 -> "EXCELLENT"
        timeInRange >= 50 -> "BON"
        timeInRange >= 30 -> "MOYEN"
        else -> "MAUVAIS"
    }
}
