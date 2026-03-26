package com.diabeto.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.entity.*
import com.diabeto.data.repository.ChatbotRepository
import com.diabeto.data.repository.GlucoseRepository
import com.diabeto.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Onglet actif dans l'écran de suivi
 */
enum class GlucoseTab {
    GLYCEMIE, HBA1C, SUIVI
}

/**
 * Moyenne glycémique journalière pour le diagramme de suivi
 */
data class DailyGlucoseAverage(
    val date: LocalDate,
    val average: Double,
    val count: Int
)

data class GlucoseUiState(
    val patient: PatientEntity? = null,
    val lectures: List<LectureGlucoseEntity> = emptyList(),
    val statistics: GlucoseStatistics = GlucoseStatistics(),
    val newValeur: String = "",
    val selectedContexte: ContexteGlucose = ContexteGlucose.AUTRE,
    val showGraph: Boolean = true,
    val isLoading: Boolean = false,
    val addSuccess: Boolean = false,
    val error: String? = null,
    // HbA1c
    val activeTab: GlucoseTab = GlucoseTab.GLYCEMIE,
    val hba1cHistorique: List<HbA1cEntity> = emptyList(),
    val latestHbA1c: HbA1cEntity? = null,
    val hba1cEstimee: Double? = null,
    val newHbA1cValeur: String = "",
    val newHbA1cLabo: String = "",
    val showAddHbA1cDialog: Boolean = false,
    // Suivi (diagramme double courbe)
    val dailyAverages: List<DailyGlucoseAverage> = emptyList(),
    // ROLLY auto-analyse
    val rollyAnalysis: String? = null,
    val showRollyAnalysis: Boolean = false,
    // Export
    val exportCsvData: String? = null
)

@HiltViewModel
class GlucoseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val glucoseRepository: GlucoseRepository,
    private val patientRepository: PatientRepository,
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    private val patientId: Long = savedStateHandle["patientId"] ?: 0L

    private val _uiState = MutableStateFlow(GlucoseUiState())
    val uiState: StateFlow<GlucoseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val patient = patientRepository.getPatientById(patientId)
                val lectures = glucoseRepository.getLecturesByPatientList(patientId, 50)
                val stats = glucoseRepository.getStatistics(patientId, 30)
                val hba1cList = glucoseRepository.getHbA1cByPatientList(patientId)
                val latestHbA1c = hba1cList.firstOrNull()
                val hba1cEstimee = if (stats.totalLectures >= 10 && stats.moyenne > 0) {
                    val estimated = HbA1cEntity.estimerDepuisGlycemieMoyenne(stats.moyenne)
                    (estimated * 10).toInt() / 10.0
                } else null

                // Calculer les moyennes journalières pour le diagramme de suivi
                val dailyAvg = lectures.groupBy { it.dateHeure.toLocalDate() }
                    .map { (date, dayLectures) ->
                        DailyGlucoseAverage(
                            date = date,
                            average = dayLectures.map { it.valeur }.average(),
                            count = dayLectures.size
                        )
                    }
                    .sortedBy { it.date }

                _uiState.update {
                    it.copy(
                        patient = patient, lectures = lectures, statistics = stats,
                        isLoading = false, hba1cHistorique = hba1cList,
                        latestHbA1c = latestHbA1c, hba1cEstimee = hba1cEstimee,
                        dailyAverages = dailyAvg
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setActiveTab(tab: GlucoseTab) = _uiState.update { it.copy(activeTab = tab) }

    fun onValeurChange(value: String) {
        _uiState.update { it.copy(newValeur = value.filter { c -> c.isDigit() || c == '.' }) }
    }
    fun onContexteChange(contexte: ContexteGlucose) = _uiState.update { it.copy(selectedContexte = contexte) }
    fun toggleViewMode() = _uiState.update { it.copy(showGraph = !it.showGraph) }

    fun addLecture() {
        viewModelScope.launch {
            try {
                val valeur = _uiState.value.newValeur.toDoubleOrNull()
                if (valeur == null || valeur <= 0) {
                    _uiState.update { it.copy(error = "Valeur invalide") }; return@launch
                }
                if (valeur > 600) {
                    _uiState.update { it.copy(error = "Valeur trop élevée") }; return@launch
                }
                glucoseRepository.insertLecture(LectureGlucoseEntity(
                    patientId = patientId, valeur = valeur,
                    dateHeure = LocalDateTime.now(), contexte = _uiState.value.selectedContexte
                ))
                _uiState.update { it.copy(newValeur = "", addSuccess = true) }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteLecture(lecture: LectureGlucoseEntity) {
        viewModelScope.launch {
            try { glucoseRepository.deleteLecture(lecture); loadData() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    // ── HbA1c ──
    fun onHbA1cValeurChange(value: String) = _uiState.update { it.copy(newHbA1cValeur = value.filter { c -> c.isDigit() || c == '.' }) }
    fun onHbA1cLaboChange(value: String) = _uiState.update { it.copy(newHbA1cLabo = value) }
    fun showAddHbA1cDialog() = _uiState.update { it.copy(showAddHbA1cDialog = true) }
    fun hideAddHbA1cDialog() = _uiState.update { it.copy(showAddHbA1cDialog = false, newHbA1cValeur = "", newHbA1cLabo = "") }

    fun addHbA1c() {
        viewModelScope.launch {
            try {
                val valeur = _uiState.value.newHbA1cValeur.toDoubleOrNull()
                if (valeur == null || valeur < 3.0 || valeur > 20.0) {
                    _uiState.update { it.copy(error = "HbA1c invalide (3% à 20%)") }; return@launch
                }
                glucoseRepository.insertHbA1c(HbA1cEntity(
                    patientId = patientId, valeur = valeur, dateMesure = LocalDate.now(),
                    laboratoire = _uiState.value.newHbA1cLabo.trim(), estEstimation = false
                ))
                _uiState.update { it.copy(showAddHbA1cDialog = false, newHbA1cValeur = "", newHbA1cLabo = "", addSuccess = true) }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteHbA1c(hba1c: HbA1cEntity) {
        viewModelScope.launch {
            try { glucoseRepository.deleteHbA1c(hba1c); loadData() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun sauvegarderEstimation() {
        viewModelScope.launch {
            try {
                val result = glucoseRepository.estimerEtSauvegarderHbA1c(patientId)
                if (result != null) { _uiState.update { it.copy(addSuccess = true) }; loadData() }
                else _uiState.update { it.copy(error = "Minimum 10 lectures pour estimer l'HbA1c") }
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun getGlucoseColor(valeur: Double): androidx.compose.ui.graphics.Color = when {
        valeur < 70 -> androidx.compose.ui.graphics.Color(0xFFE57373)
        valeur > 180 -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
        else -> androidx.compose.ui.graphics.Color(0xFF81C784)
    }

    fun getGlucoseStatus(valeur: Double): String = when {
        valeur < 54 -> "Hypoglycémie sévère"
        valeur < 70 -> "Hypoglycémie"
        valeur in 70.0..180.0 -> "Dans la cible"
        valeur in 180.0..250.0 -> "Hyperglycémie"
        else -> "Hyperglycémie sévère"
    }

    fun getHbA1cColor(valeur: Double): androidx.compose.ui.graphics.Color = when {
        valeur < 5.7 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        valeur < 6.5 -> androidx.compose.ui.graphics.Color(0xFFFFC107)
        valeur < 7.0 -> androidx.compose.ui.graphics.Color(0xFF81C784)
        valeur < 8.0 -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
        valeur < 9.0 -> androidx.compose.ui.graphics.Color(0xFFE57373)
        else -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearAddSuccess() = _uiState.update { it.copy(addSuccess = false) }
    fun refresh() = loadData()

    // ── ROLLY Auto-analyse après ajout de données ──
    fun triggerRollyAnalysis() {
        viewModelScope.launch {
            try {
                val patient = _uiState.value.patient ?: return@launch
                val lectures = glucoseRepository.getLast7DaysLectures(patientId)
                if (lectures.size < 3) return@launch
                val latestHbA1c = _uiState.value.hba1cHistorique.firstOrNull { !it.estEstimation }
                val hba1cEst = _uiState.value.hba1cEstimee
                val analysis = chatbotRepository.analyserGlycemie(patient, lectures, latestHbA1c, hba1cEst)
                _uiState.update { it.copy(rollyAnalysis = analysis, showRollyAnalysis = true) }
            } catch (_: Exception) {
                // Silencieux : l'auto-analyse est optionnelle
            }
        }
    }

    fun dismissRollyAnalysis() = _uiState.update { it.copy(showRollyAnalysis = false, rollyAnalysis = null) }

    // ── Export CSV ──
    fun generateExportData() {
        viewModelScope.launch {
            try {
                val patient = _uiState.value.patient
                val sb = StringBuilder()
                val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

                sb.appendLine("=== RAPPORT DIASMART ===")
                patient?.let {
                    sb.appendLine("Patient: ${it.nomComplet}")
                    sb.appendLine("Age: ${it.age} ans | Sexe: ${it.sexe.name}")
                    sb.appendLine("Type: ${it.typeDiabete.name.replace("_", " ")}")
                    it.imc?.let { imc -> sb.appendLine("IMC: ${"%.1f".format(imc)} kg/m² (${it.categorieImc})") }
                    it.tourDeTaille?.let { tdt -> sb.appendLine("Tour de taille: ${tdt}cm (${it.risqueTourDeTaille})") }
                    it.masseGrasse?.let { mg -> sb.appendLine("Masse grasse: ${mg}%") }
                }
                sb.appendLine()

                // Statistiques
                val stats = _uiState.value.statistics
                sb.appendLine("=== STATISTIQUES (30 jours) ===")
                sb.appendLine("Moyenne: ${stats.moyenne.toInt()} mg/dL")
                sb.appendLine("Min-Max: ${stats.minimum.toInt()}-${stats.maximum.toInt()} mg/dL")
                sb.appendLine("TIR (70-180): ${stats.timeInRange.toInt()}%")
                sb.appendLine("Total lectures: ${stats.totalLectures}")
                sb.appendLine()

                // HbA1c
                sb.appendLine("=== HbA1c ===")
                _uiState.value.hba1cHistorique.forEach { h ->
                    val type = if (h.estEstimation) "estimée" else "labo"
                    sb.appendLine("${h.dateMesure.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}: ${h.valeur}% ($type) - eAG: ${h.getGlycemieMoyenneEstimee().toInt()} mg/dL")
                }
                sb.appendLine()

                // Lectures CSV
                sb.appendLine("=== LECTURES GLYCEMIE ===")
                sb.appendLine("Date,Valeur (mg/dL),Contexte")
                _uiState.value.lectures.forEach { l ->
                    sb.appendLine("${l.dateHeure.format(dateFmt)},${l.valeur.toInt()},${l.contexte.getDisplayName()}")
                }

                _uiState.update { it.copy(exportCsvData = sb.toString()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erreur export: ${e.message}") }
            }
        }
    }

    fun clearExportData() = _uiState.update { it.copy(exportCsvData = null) }
}
