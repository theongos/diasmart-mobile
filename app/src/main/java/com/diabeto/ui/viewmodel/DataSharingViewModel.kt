package com.diabeto.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.model.DataSharingConsent
import com.diabeto.data.model.UserProfile
import com.diabeto.data.model.UserRole
import com.diabeto.data.repository.AuthRepository
import com.diabeto.data.repository.DataSharingRepository
import com.diabeto.data.repository.GlucoseRepository
import com.diabeto.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DataSharingUiState(
    val isPatient: Boolean = true,
    val consents: List<DataSharingConsent> = emptyList(),
    val availableMedecins: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val exportData: String? = null
)

@HiltViewModel
class DataSharingViewModel @Inject constructor(
    private val dataSharingRepository: DataSharingRepository,
    private val authRepository: AuthRepository,
    private val patientRepository: PatientRepository,
    private val glucoseRepository: GlucoseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSharingUiState())
    val uiState: StateFlow<DataSharingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val profile = authRepository.getCurrentUserProfile()
                val isPatient = profile?.role != UserRole.MEDECIN

                val consents = if (isPatient) {
                    dataSharingRepository.getMyConsents()
                } else {
                    dataSharingRepository.getSharedPatients()
                }

                _uiState.update {
                    it.copy(
                        isPatient = isPatient,
                        consents = consents,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Erreur: ${e.message}") }
            }
        }
    }

    fun loadMedecins() {
        viewModelScope.launch {
            try {
                val medecins = dataSharingRepository.getAvailableMedecins()
                _uiState.update { it.copy(availableMedecins = medecins) }
            } catch (e: Exception) {
                Log.w("DataSharingVM", "Failed to load medecins", e)
            }
        }
    }

    fun grantConsent(medecinUid: String) {
        viewModelScope.launch {
            val result = dataSharingRepository.grantConsent(medecinUid)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Accès accordé avec succès") }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(message = "Erreur: ${e.message}") }
                }
            )
        }
    }

    fun revokeConsent(medecinUid: String) {
        viewModelScope.launch {
            val result = dataSharingRepository.revokeConsent(medecinUid)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Accès révoqué") }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(message = "Erreur: ${e.message}") }
                }
            )
        }
    }

    fun generateExportData(format: String) {
        viewModelScope.launch {
            try {
                val patients = patientRepository.getAllPatientsList()
                val sb = StringBuilder()
                val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

                if (format == "csv") {
                    // CSV format
                    sb.appendLine("Date,Glycémie (mg/dL),Contexte,HbA1c (%),Type HbA1c")
                    patients.forEach { patient ->
                        val lectures = glucoseRepository.getLecturesByPatientList(patient.id, 200)
                        lectures.forEach { l ->
                            sb.appendLine("${l.dateHeure.format(dateFmt)},${l.valeur.toInt()},${l.contexte.getDisplayName()},,")
                        }
                        val hba1cList = glucoseRepository.getHbA1cByPatientList(patient.id)
                        hba1cList.forEach { h ->
                            sb.appendLine("${h.dateMesure.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))},,,${h.valeur},${if (h.estEstimation) "estimée" else "labo"}")
                        }
                    }
                } else {
                    // Text report
                    sb.appendLine("═══════════════════════════════════════")
                    sb.appendLine("     RAPPORT DIASMART")
                    sb.appendLine("     Généré le ${java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                    sb.appendLine("═══════════════════════════════════════")
                    sb.appendLine()

                    patients.forEach { patient ->
                        sb.appendLine("── Patient: ${patient.nomComplet} ──")
                        sb.appendLine("Age: ${patient.age} ans | Sexe: ${patient.sexe.name}")
                        sb.appendLine("Diabète: ${patient.typeDiabete.name.replace("_", " ")}")
                        patient.imc?.let { sb.appendLine("IMC: ${"%.1f".format(it)} (${patient.categorieImc})") }
                        sb.appendLine()

                        val stats = glucoseRepository.getStatistics(patient.id, 30)
                        sb.appendLine("Statistiques (30j):")
                        sb.appendLine("  Moyenne: ${stats.moyenne.toInt()} mg/dL")
                        sb.appendLine("  TIR: ${stats.timeInRange.toInt()}%")
                        sb.appendLine("  Min: ${stats.minimum.toInt()} | Max: ${stats.maximum.toInt()}")
                        sb.appendLine()

                        val hba1cList = glucoseRepository.getHbA1cByPatientList(patient.id)
                        if (hba1cList.isNotEmpty()) {
                            sb.appendLine("HbA1c:")
                            hba1cList.take(5).forEach { h ->
                                sb.appendLine("  ${h.dateMesure}: ${h.valeur}% (${if (h.estEstimation) "estimée" else "labo"})")
                            }
                            sb.appendLine()
                        }

                        val lectures = glucoseRepository.getLecturesByPatientList(patient.id, 30)
                        sb.appendLine("Dernières lectures:")
                        lectures.take(20).forEach { l ->
                            sb.appendLine("  ${l.dateHeure.format(dateFmt)} : ${l.valeur.toInt()} mg/dL (${l.contexte.getDisplayName()})")
                        }
                        sb.appendLine()
                    }

                    sb.appendLine("═══════════════════════════════════════")
                    sb.appendLine("Avis informatif — consultez votre médecin.")
                }

                _uiState.update { it.copy(exportData = sb.toString()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Erreur export: ${e.message}") }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearExportData() = _uiState.update { it.copy(exportData = null) }
}
