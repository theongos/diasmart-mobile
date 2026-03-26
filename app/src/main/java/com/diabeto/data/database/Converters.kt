package com.diabeto.data.database

import androidx.room.TypeConverter
import com.diabeto.data.entity.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Converters pour les types complexes Room
 */
class Converters {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    
    // LocalDate converters
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }
    
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it, dateFormatter) }
    }
    
    // LocalDateTime converters
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }
    
    // LocalTime converters
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }
    
    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let { LocalTime.parse(it, timeFormatter) }
    }
    
    // Sexe enum converters
    @TypeConverter
    fun fromSexe(sexe: Sexe?): String? {
        return sexe?.name
    }
    
    @TypeConverter
    fun toSexe(sexeString: String?): Sexe? {
        return sexeString?.let { Sexe.valueOf(it) }
    }
    
    // TypeDiabete enum converters
    @TypeConverter
    fun fromTypeDiabete(type: TypeDiabete?): String? {
        return type?.name
    }
    
    @TypeConverter
    fun toTypeDiabete(typeString: String?): TypeDiabete? {
        return typeString?.let { TypeDiabete.valueOf(it) }
    }
    
    // ContexteGlucose enum converters
    @TypeConverter
    fun fromContexteGlucose(contexte: ContexteGlucose?): String? {
        return contexte?.name
    }
    
    @TypeConverter
    fun toContexteGlucose(contexteString: String?): ContexteGlucose? {
        return contexteString?.let { ContexteGlucose.valueOf(it) }
    }
    
    // FrequencePrise enum converters
    @TypeConverter
    fun fromFrequencePrise(frequence: FrequencePrise?): String? {
        return frequence?.name
    }
    
    @TypeConverter
    fun toFrequencePrise(frequenceString: String?): FrequencePrise? {
        return frequenceString?.let { FrequencePrise.valueOf(it) }
    }
    
    // TypeRendezVous enum converters
    @TypeConverter
    fun fromTypeRendezVous(type: TypeRendezVous?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTypeRendezVous(typeString: String?): TypeRendezVous? {
        return typeString?.let { TypeRendezVous.valueOf(it) }
    }

    // Humeur enum converters
    @TypeConverter
    fun fromHumeur(humeur: Humeur?): String? = humeur?.name

    @TypeConverter
    fun toHumeur(str: String?): Humeur? = str?.let { Humeur.valueOf(it) }

    // NiveauStress enum converters
    @TypeConverter
    fun fromNiveauStress(stress: NiveauStress?): String? = stress?.name

    @TypeConverter
    fun toNiveauStress(str: String?): NiveauStress? = str?.let { NiveauStress.valueOf(it) }

    // QualiteSommeil enum converters
    @TypeConverter
    fun fromQualiteSommeil(sommeil: QualiteSommeil?): String? = sommeil?.name

    @TypeConverter
    fun toQualiteSommeil(str: String?): QualiteSommeil? = str?.let { QualiteSommeil.valueOf(it) }
}
