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
    
    suspend fun insertMedicament(medicament: MedicamentEntity): Long {
        val id = medicamentDao.insertMedicament(medicament)
        try { cloudBackup.backupMedicament(medicament.copy(id = id)) } catch (_: Exception) {}
        return id
    }

    suspend fun insertMedicaments(medicaments: List<MedicamentEntity>) {
        medicamentDao.insertMedicaments(medicaments)
        try { for (m in medicaments) cloudBackup.backupMedicament(m) } catch (_: Exception) {}
    }

    suspend fun updateMedicament(medicament: MedicamentEntity) {
        medicamentDao.updateMedicament(medicament)
        try { cloudBackup.backupMedicament(medicament) } catch (_: Exception) {}
    }

    suspend fun deleteMedicament(medicament: MedicamentEntity) {
        medicamentDao.deleteMedicament(medicament)
        try { cloudBackup.deleteBackupDoc("medicaments", medicament.id.toString()) } catch (_: Exception) {}
    }

    suspend fun deleteMedicamentById(id: Long) {
        medicamentDao.deleteMedicamentById(id)
        try { cloudBackup.deleteBackupDoc("medicaments", id.toString()) } catch (_: Exception) {}
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
