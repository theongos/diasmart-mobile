package com.diabeto.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Entité RendezVous pour Room Database
 */
@Entity(
    tableName = "rendez_vous",
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
        Index(value = ["dateHeure"]),
        Index(value = ["estConfirme"])
    ]
)
data class RendezVousEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val titre: String,
    val dateHeure: LocalDateTime,
    val dureeMinutes: Int = 30,
    val type: TypeRendezVous,
    val lieu: String = "",
    val notes: String = "",
    val estConfirme: Boolean = false,
    val rappelEnvoye: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastModified: Long = System.currentTimeMillis()
) {
    fun estPasse(): Boolean = dateHeure.isBefore(LocalDateTime.now())
    
    fun estAujourdhui(): Boolean {
        val now = LocalDateTime.now()
        return dateHeure.toLocalDate() == now.toLocalDate()
    }
    
    fun estCetteSemaine(): Boolean {
        val now = LocalDateTime.now()
        val daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), dateHeure.toLocalDate())
        return daysUntil in 0..7
    }
    
    fun getTempsRestant(): String {
        val now = LocalDateTime.now()
        if (dateHeure.isBefore(now)) return "Passé"
        
        val minutes = ChronoUnit.MINUTES.between(now, dateHeure)
        val hours = ChronoUnit.HOURS.between(now, dateHeure)
        val days = ChronoUnit.DAYS.between(now, dateHeure)
        
        return when {
            minutes < 60 -> "Dans $minutes min"
            hours < 24 -> "Dans $hours h"
            days < 7 -> "Dans $days j"
            else -> "Dans ${days / 7} sem"
        }
    }
    
    fun getUrgenceLevel(): Int = when {
        estPasse() -> 0
        estAujourdhui() -> 3 // Urgent
        estCetteSemaine() -> 2 // Important
        else -> 1 // Normal
    }
}

enum class TypeRendezVous {
    CONSULTATION,
    EXAMEN,
    SUIVI,
    URGENCE,
    TELECONSULTATION;

    fun getDisplayName(): String = when (this) {
        CONSULTATION -> "Consultation"
        EXAMEN -> "Examen"
        SUIVI -> "Suivi"
        URGENCE -> "Urgence"
        TELECONSULTATION -> "Téléconsultation"
    }
    
    fun getIcon(): String = when (this) {
        CONSULTATION -> "stethoscope"
        EXAMEN -> "science"
        SUIVI -> "monitor_heart"
        URGENCE -> "emergency"
        TELECONSULTATION -> "videocam"
    }
}

/**
 * Classe pour les rendez-vous avec info patient
 */
data class RendezVousAvecPatient(
    @Embedded val rendezVous: RendezVousEntity,
    @Relation(
        parentColumn = "patientId",
        entityColumn = "id"
    )
    val patient: PatientEntity
)
