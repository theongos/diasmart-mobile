package com.diabeto.data.repository

import com.diabeto.data.dao.JournalDao
import com.diabeto.data.entity.JournalEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val journalDao: JournalDao,
    private val cloudBackup: CloudBackupRepository
) {
    fun getEntriesByPatient(patientId: Long): Flow<List<JournalEntity>> =
        journalDao.getEntriesByPatient(patientId)

    suspend fun getEntriesByPatientList(patientId: Long): List<JournalEntity> =
        journalDao.getEntriesByPatientList(patientId)

    suspend fun getEntryByDate(patientId: Long, date: LocalDate): JournalEntity? =
        journalDao.getEntryByDate(patientId, date)

    suspend fun getEntryById(id: Long): JournalEntity? =
        journalDao.getEntryById(id)

    // Écritures LOCAL-FIRST — sync par BatchSyncWorker (toutes les 4h)

    suspend fun saveEntry(entry: JournalEntity): Long {
        return journalDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: JournalEntity) {
        journalDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: JournalEntity) {
        journalDao.deleteEntry(entry)
    }

    suspend fun getEntriesBetweenDates(patientId: Long, startDate: LocalDate, endDate: LocalDate): List<JournalEntity> =
        journalDao.getEntriesBetweenDates(patientId, startDate, endDate)

    suspend fun getEntryCount(patientId: Long): Int =
        journalDao.getEntryCount(patientId)
}
