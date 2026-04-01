package com.diabeto.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class Humeur(val label: String, val emoji: String) {
    TRES_BIEN("Très bien", "😄"),
    BIEN("Bien", "🙂"),
    NEUTRE("Neutre", "😐"),
    MAL("Mal", "😟"),
    TRES_MAL("Très mal", "😢")
}

enum class NiveauStress(val label: String, val valeur: Int) {
    AUCUN("Aucun", 0),
    LEGER("Léger", 1),
    MODERE("Modéré", 2),
    ELEVE("Élevé", 3),
    EXTREME("Extrême", 4)
}

enum class QualiteSommeil(val label: String, val emoji: String) {
    EXCELLENTE("Excellente", "😴"),
    BONNE("Bonne", "🌙"),
    MOYENNE("Moyenne", "😑"),
    MAUVAISE("Mauvaise", "😫"),
    INSOMNIE("Insomnie", "😵")
}

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId"), Index("date")]
)
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val date: LocalDate = LocalDate.now(),
    val humeur: Humeur = Humeur.NEUTRE,
    val niveauStress: NiveauStress = NiveauStress.AUCUN,
    val qualiteSommeil: QualiteSommeil = QualiteSommeil.BONNE,
    val heuresSommeil: Double = 7.0,
    val activitePhysique: Boolean = false,
    val minutesActivite: Int = 0,
    val pas: Int = 0,
    val notes: String = "",
    val glycemieCorrelation: Double? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastModified: Long = System.currentTimeMillis()
)
