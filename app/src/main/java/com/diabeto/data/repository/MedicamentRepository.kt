package com.diabeto.data.repository

import com.diabeto.data.dao.MedicamentDao
import com.diabeto.data.entity.MedicamentEntity
import com.diabeto.data.entity.RappelMedicament
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour les opérations sur les médicaments
 */
@Singleton
class MedicamentRepository @Inject constructor(
    private val medicamentDao: MedicamentDao,
    private val cloudBackup: CloudBackupRepository
) {
    fun getMedicamentsByPatient(patientId: Long): Flow<List<MedicamentEntity>> = 
        medicamentDao.getMedicamentsByPatient(patientId)
    
    suspend fun getMedicamentsByPatientList(patientId: Long): List<MedicamentEntity> = 
        medicamentDao.getMedicamentsByPatientList(patientId)
    
    suspend fun getMedicamentById(id: Long): MedicamentEntity? = 
        medicamentDao.getMedicamentById(id)
    
    suspend fun getActiveMedicamentsWithReminders(): List<MedicamentEntity> = 
        medicamentDao.getActiveMedicamentsWithReminders()
    
    suspend fun getRappelsForHourRange(hourStart: Int, hourEnd: Int): List<RappelMedicament> = 
        medicamentDao.getRappelsForHourRange(hourStart, hourEnd)
    
    fun getAllRappelsFlow(): Flow<List<RappelMedicament>> = 
        medicamentDao.getAllRappelsFlow()
    
    suspend fun getActiveMedicamentCount(patientId: Long): Int = 
        medicamentDao.getActiveMedicamentCount(patientId)
    
    // Écritures LOCAL-FIRST — sync par BatchSyncWorker (toutes les 4h)

    suspend fun insertMedicament(medicament: MedicamentEntity): Long {
        return medicamentDao.insertMedicament(medicament)
    }

    suspend fun insertMedicaments(medicaments: List<MedicamentEntity>) {
        medicamentDao.insertMedicaments(medicaments)
    }

    suspend fun updateMedicament(medicament: MedicamentEntity) {
        medicamentDao.updateMedicament(medicament)
    }

    suspend fun deleteMedicament(medicament: MedicamentEntity) {
        medicamentDao.deleteMedicament(medicament)
    }

    suspend fun deleteMedicamentById(id: Long) {
        medicamentDao.deleteMedicamentById(id)
    }

    suspend fun deleteAllPatientMedicaments(patientId: Long) =
        medicamentDao.deleteAllPatientMedicaments(patientId)
    
    suspend fun toggleActifStatus(id: Long, currentStatus: Boolean) = 
        medicamentDao.updateActifStatus(id, !currentStatus)
    
    suspend fun toggleRappelStatus(id: Long, currentStatus: Boolean) = 
        medicamentDao.updateRappelStatus(id, !currentStatus)
    
    /**
     * Récupère les médicaments à prendre prochainement (dans les 2 heures)
     */
    suspend fun getUpcomingMedicaments(): List<RappelMedicament> {
        val now = java.time.LocalTime.now().hour
        return getRappelsForHourRange(now, now + 2)
    }
}
