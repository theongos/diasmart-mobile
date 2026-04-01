package com.diabeto.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Entité Medicament pour Room Database
 */
@Entity(
    tableName = "medicaments",
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
        Index(value = ["estActif"])
    ]
)
data class MedicamentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val nom: String,
    val dosage: String,
    val frequence: FrequencePrise,
    val heurePrise: LocalTime,
    val dateDebut: LocalDate,
    val dateFin: LocalDate? = null,
    val estActif: Boolean = true,
    val rappelActive: Boolean = true,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastModified: Long = System.currentTimeMillis()
) {
    fun estEnCours(): Boolean {
        val today = LocalDate.now()
        return estActif && 
               !dateDebut.isAfter(today) && 
               (dateFin == null || !dateFin.isBefore(today))
    }
    
    fun getProchainePrise(): LocalDateTime? {
        if (!estEnCours()) return null
        val today = LocalDate.now()
        val now = LocalTime.now()
        
        return if (heurePrise.isAfter(now)) {
            LocalDateTime.of(today, heurePrise)
        } else {
            LocalDateTime.of(today.plusDays(1), heurePrise)
        }
    }
}

enum class FrequencePrise {
    QUOTIDIEN,
    BID, // 2 fois par jour
    TID, // 3 fois par jour
    QID, // 4 fois par jour
    HEBDOMADAIRE,
    MENSUEL,
    AU_BESOIN;

    fun getDisplayName(): String = when (this) {
        QUOTIDIEN -> "1 fois par jour"
        BID -> "2 fois par jour"
        TID -> "3 fois par jour"
        QID -> "4 fois par jour"
        HEBDOMADAIRE -> "1 fois par semaine"
        MENSUEL -> "1 fois par mois"
        AU_BESOIN -> "Au besoin"
    }
}

/**
 * Classe pour les rappels de médicaments avec info patient
 */
data class RappelMedicament(
    @Embedded val medicament: MedicamentEntity,
    @Relation(
        parentColumn = "patientId",
        entityColumn = "id"
    )
    val patient: PatientEntity
)
