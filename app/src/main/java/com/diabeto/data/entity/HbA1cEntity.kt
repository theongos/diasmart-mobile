package com.diabeto.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Entité HbA1c (Hémoglobine Glyquée) pour Room Database
 *
 * L'HbA1c reflète la glycémie moyenne des 2-3 derniers mois.
 * C'est l'indicateur clé du contrôle du diabète.
 *
 * Cibles recommandées (ADA 2024) :
 *   - Adultes : < 7.0%
 *   - Personnes âgées / comorbidités : < 8.0%
 *   - Grossesse : < 6.0%
 *   - Sans hypoglycémies fréquentes : < 6.5%
 */
@Entity(
    tableName = "hba1c_lectures",
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
        Index(value = ["dateMesure"])
    ]
)
data class HbA1cEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val valeur: Double, // Pourcentage (ex: 6.8)
    val dateMesure: LocalDate,
    val laboratoire: String = "",
    val notes: String = "",
    val estEstimation: Boolean = false, // true si calculée à partir de la glycémie moyenne
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastModified: Long = System.currentTimeMillis()
) {
    /**
     * Interprétation clinique de la valeur HbA1c
     */
    fun getInterpretation(): HbA1cInterpretation = when {
        valeur < 5.7 -> HbA1cInterpretation.NORMAL
        valeur in 5.7..6.4 -> HbA1cInterpretation.PREDIABETE
        valeur in 6.5..7.0 -> HbA1cInterpretation.CIBLE_ATTEINTE
        valeur in 7.0..8.0 -> HbA1cInterpretation.AU_DESSUS_CIBLE
        valeur in 8.0..9.0 -> HbA1cInterpretation.MAUVAIS_CONTROLE
        else -> HbA1cInterpretation.TRES_MAUVAIS_CONTROLE
    }

    /**
     * Glycémie moyenne estimée (eAG) à partir de l'HbA1c
     * Formule ADAG : eAG (mg/dL) = 28.7 × HbA1c − 46.7
     */
    fun getGlycemieMoyenneEstimee(): Double = 28.7 * valeur - 46.7

    companion object {
        /**
         * Estimer l'HbA1c à partir de la glycémie moyenne (mg/dL)
         * Formule inverse ADAG : HbA1c = (eAG + 46.7) / 28.7
         */
        fun estimerDepuisGlycemieMoyenne(glycemieMoyenne: Double): Double {
            return (glycemieMoyenne + 46.7) / 28.7
        }
    }
}

enum class HbA1cInterpretation {
    NORMAL,              // < 5.7%
    PREDIABETE,          // 5.7 - 6.4%
    CIBLE_ATTEINTE,      // 6.5 - 7.0%
    AU_DESSUS_CIBLE,     // 7.0 - 8.0%
    MAUVAIS_CONTROLE,    // 8.0 - 9.0%
    TRES_MAUVAIS_CONTROLE; // > 9.0%

    fun getDisplayName(): String = when (this) {
        NORMAL -> "Normal"
        PREDIABETE -> "Prédiabète"
        CIBLE_ATTEINTE -> "Cible atteinte"
        AU_DESSUS_CIBLE -> "Au-dessus de la cible"
        MAUVAIS_CONTROLE -> "Mauvais contrôle"
        TRES_MAUVAIS_CONTROLE -> "Très mauvais contrôle"
    }

    fun getDescription(): String = when (this) {
        NORMAL -> "Glycémie dans les valeurs normales"
        PREDIABETE -> "Risque de diabète, surveillance recommandée"
        CIBLE_ATTEINTE -> "Bon contrôle du diabète, continuez ainsi"
        AU_DESSUS_CIBLE -> "Ajustement thérapeutique recommandé"
        MAUVAIS_CONTROLE -> "Risque accru de complications, consultez"
        TRES_MAUVAIS_CONTROLE -> "Urgence : consultation médicale nécessaire"
    }
}
