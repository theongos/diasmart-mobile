package com.diabeto.data.repository

import com.diabeto.data.dao.PatientDao
import com.diabeto.data.entity.PatientEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour les opérations sur les patients
 */
@Singleton
class PatientRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val cloudBackup: CloudBackupRepository
) {
    fun getAllPatients(): Flow<List<PatientEntity>> = patientDao.getAllPatients()

    suspend fun getAllPatientsList(): List<PatientEntity> = patientDao.getAllPatientsList()

    suspend fun getPatientById(id: Long): PatientEntity? = patientDao.getPatientById(id)

    fun getPatientByIdFlow(id: Long): Flow<PatientEntity?> = patientDao.getPatientByIdFlow(id)

    fun searchPatients(query: String): Flow<List<PatientEntity>> =
        patientDao.searchPatients(query)

    suspend fun getPatientCount(): Int = patientDao.getPatientCount()

    suspend fun insertPatient(patient: PatientEntity): Long {
        val id = patientDao.insertPatient(patient)
        try { cloudBackup.backupPatient(patient.copy(id = id)) } catch (_: Exception) {}
        return id
    }

    suspend fun updatePatient(patient: PatientEntity) {
        patientDao.updatePatient(patient)
        try { cloudBackup.backupPatient(patient) } catch (_: Exception) {}
    }

    suspend fun deletePatient(patient: PatientEntity) {
        patientDao.deletePatient(patient)
        try { cloudBackup.deleteBackupDoc("patients", patient.id.toString()) } catch (_: Exception) {}
    }

    suspend fun deletePatientById(id: Long) {
        patientDao.deletePatientById(id)
        try { cloudBackup.deleteBackupDoc("patients", id.toString()) } catch (_: Exception) {}
    }
}
