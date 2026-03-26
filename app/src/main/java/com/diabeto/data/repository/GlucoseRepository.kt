package com.diabeto.data.repository

import com.diabeto.data.dao.GlucoseDao
import com.diabeto.data.dao.HbA1cDao
import com.diabeto.data.entity.GlucoseStatistics
import com.diabeto.data.entity.HbA1cEntity
import com.diabeto.data.entity.LectureGlucoseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour les opérations sur les lectures de glucose et l'HbA1c
 */
@Singleton
class GlucoseRepository @Inject constructor(
    private val glucoseDao: GlucoseDao,
    private val hbA1cDao: HbA1cDao,
    private val cloudBackup: CloudBackupRepository
) {
    // ================================================================
    //  GLYCÉMIE
    // ================================================================

    fun getLecturesByPatient(patientId: Long, limit: Int = 100): Flow<List<LectureGlucoseEntity>> =
        glucoseDao.getLecturesByPatient(patientId, limit)

    suspend fun getLecturesByPatientList(patientId: Long, limit: Int = 100): List<LectureGlucoseEntity> =
        glucoseDao.getLecturesByPatientList(patientId, limit)

    fun getLecturesBetweenDates(
        patientId: Long,
        fromDate: LocalDateTime
    ): Flow<List<LectureGlucoseEntity>> =
        glucoseDao.getLecturesBetweenDates(patientId, fromDate)

    suspend fun getStatistics(patientId: Long, days: Int = 30): GlucoseStatistics {
        val fromDate = LocalDateTime.now().minusDays(days.toLong())
        return glucoseDao.getStatistics(patientId, fromDate) ?: GlucoseStatistics()
    }

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun getLast7DaysLectures(patientId: Long): List<LectureGlucoseEntity> {
        val fromDate = LocalDateTime.now().minusDays(7).format(isoFormatter)
        return glucoseDao.getLast7DaysLectures(patientId, fromDate)
    }

    suspend fun getLast30DaysLectures(patientId: Long): List<LectureGlucoseEntity> {
        val fromDate = LocalDateTime.now().minusDays(30).format(isoFormatter)
        return glucoseDao.getLast30DaysLectures(patientId, fromDate)
    }

    suspend fun getLast24HoursAverage(patientId: Long): Double {
        val fromDate = LocalDateTime.now().minusHours(24).format(isoFormatter)
        return glucoseDao.getLast24HoursAverage(patientId, fromDate) ?: 0.0
    }

    suspend fun getLatestReading(patientId: Long): LectureGlucoseEntity? =
        glucoseDao.getLatestReading(patientId)

    suspend fun insertLecture(lecture: LectureGlucoseEntity): Long {
        val id = glucoseDao.insertLecture(lecture)
        try { cloudBackup.backupGlucose(lecture.copy(id = id)) } catch (_: Exception) {}
        return id
    }

    suspend fun insertLectures(lectures: List<LectureGlucoseEntity>) {
        glucoseDao.insertLectures(lectures)
        try { for (l in lectures) cloudBackup.backupGlucose(l) } catch (_: Exception) {}
    }

    suspend fun updateLecture(lecture: LectureGlucoseEntity) {
        glucoseDao.updateLecture(lecture)
        try { cloudBackup.backupGlucose(lecture) } catch (_: Exception) {}
    }

    suspend fun deleteLecture(lecture: LectureGlucoseEntity) {
        glucoseDao.deleteLecture(lecture)
        try { cloudBackup.deleteBackupDoc("glucose", lecture.id.toString()) } catch (_: Exception) {}
    }

    suspend fun deleteLectureById(id: Long) {
        glucoseDao.deleteLectureById(id)
        try { cloudBackup.deleteBackupDoc("glucose", id.toString()) } catch (_: Exception) {}
    }

    suspend fun deleteAllPatientLectures(patientId: Long) =
        glucoseDao.deleteAllPatientLectures(patientId)

    fun analyzeTrend(lectures: List<LectureGlucoseEntity>): TrendResult {
        if (lectures.size < 3) return TrendResult.STABLE
        val recent = lectures.take(3).map { it.valeur }
        val avgRecent = recent.average()
        val older = lectures.drop(3).take(3).map { it.valeur }
        if (older.isEmpty()) return TrendResult.STABLE
        val avgOlder = older.average()
        val diff = avgRecent - avgOlder
        return when {
            diff > 15 -> TrendResult.RISING
            diff < -15 -> TrendResult.FALLING
            else -> TrendResult.STABLE
        }
    }

    enum class TrendResult {
        RISING, FALLING, STABLE
    }

    // ================================================================
    //  HbA1c (HÉMOGLOBINE GLYQUÉE)
    // ================================================================

    fun getHbA1cByPatient(patientId: Long): Flow<List<HbA1cEntity>> =
        hbA1cDao.getHbA1cByPatient(patientId)

    suspend fun getHbA1cByPatientList(patientId: Long): List<HbA1cEntity> =
        hbA1cDao.getHbA1cByPatientList(patientId)

    suspend fun getLatestHbA1c(patientId: Long): HbA1cEntity? =
        hbA1cDao.getLatestHbA1c(patientId)

    suspend fun getHbA1cSince(patientId: Long, months: Int = 12): List<HbA1cEntity> {
        val fromDate = LocalDate.now().minusMonths(months.toLong())
        return hbA1cDao.getHbA1cSince(patientId, fromDate)
    }

    suspend fun insertHbA1c(hba1c: HbA1cEntity): Long {
        val id = hbA1cDao.insertHbA1c(hba1c)
        try { cloudBackup.backupHbA1c(hba1c.copy(id = id)) } catch (_: Exception) {}
        return id
    }

    suspend fun updateHbA1c(hba1c: HbA1cEntity) {
        hbA1cDao.updateHbA1c(hba1c)
        try { cloudBackup.backupHbA1c(hba1c) } catch (_: Exception) {}
    }

    suspend fun deleteHbA1c(hba1c: HbA1cEntity) {
        hbA1cDao.deleteHbA1c(hba1c)
        try { cloudBackup.deleteBackupDoc("hba1c", hba1c.id.toString()) } catch (_: Exception) {}
    }

    suspend fun deleteHbA1cById(id: Long) {
        hbA1cDao.deleteHbA1cById(id)
        try { cloudBackup.deleteBackupDoc("hba1c", id.toString()) } catch (_: Exception) {}
    }

    /**
     * Estimer l'HbA1c à partir des 30 derniers jours de glycémie
     * et sauvegarder comme estimation dans la base.
     * Formule ADAG : HbA1c = (glycémie moyenne + 46.7) / 28.7
     */
    suspend fun estimerEtSauvegarderHbA1c(patientId: Long): HbA1cEntity? {
        val lectures = getLast30DaysLectures(patientId)
        if (lectures.size < 10) return null

        val moyenne = lectures.map { it.valeur }.average()
        val hba1cEstimee = HbA1cEntity.estimerDepuisGlycemieMoyenne(moyenne)

        val entity = HbA1cEntity(
            patientId = patientId,
            valeur = (hba1cEstimee * 10).toInt() / 10.0,
            dateMesure = LocalDate.now(),
            laboratoire = "Estimation DiaSmart",
            notes = "Estimée à partir de ${lectures.size} lectures (moy: ${moyenne.toInt()} mg/dL)",
            estEstimation = true
        )
        insertHbA1c(entity)
        return entity
    }
}
