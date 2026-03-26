package com.diabeto.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.entity.LectureGlucoseEntity
import com.diabeto.data.repository.ChatbotRepository
import com.diabeto.data.repository.GlucoseRepository
import com.diabeto.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Point de données prédictif pour le graphique
 */
data class PredictivePoint(
    val hoursFromNow: Float,
    val value: Double,
    val isPrediction: Boolean = false,
    val confidence: Float = 1f // 1.0 = certain, 0.0 = très incertain
)

data class PredictiveUiState(
    val isLoading: Boolean = true,
    val historicalPoints: List<PredictivePoint> = emptyList(),
    val predictedPoints: List<PredictivePoint> = emptyList(),
    val currentValue: Double? = null,
    val predictedMin: Double? = null,
    val predictedMax: Double? = null,
    val trendDescription: String = "",
    val rollyAnalysis: String? = null,
    val isAnalyzing: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.NORMAL,
    val alerts: List<String> = emptyList(),
    val error: String? = null
)

enum class RiskLevel(val label: String, val color: Long) {
    LOW("Faible", 0xFF4CAF50),
    NORMAL("Normal", 0xFF2196F3),
    MODERATE("Modéré", 0xFFFF9800),
    HIGH("Élevé", 0xFFF44336),
    CRITICAL("Critique", 0xFF9C27B0)
}

@HiltViewModel
class PredictiveGlucoseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val glucoseRepository: GlucoseRepository,
    private val patientRepository: PatientRepository,
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    private val patientId: Long = savedStateHandle.get<Long>("patientId")?.takeIf { it > 0 }
        ?: -1L

    private val _uiState = MutableStateFlow(PredictiveUiState())
    val uiState: StateFlow<PredictiveUiState> = _uiState.asStateFlow()

    init {
        loadDataAndPredict()
    }

    private fun loadDataAndPredict() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val pid = if (patientId > 0) patientId else {
                    patientRepository.getAllPatientsList().firstOrNull()?.id ?: run {
                        _uiState.update { it.copy(isLoading = false, error = "Aucun patient") }
                        return@launch
                    }
                }

                // Charger les 48 dernières heures de lectures
                val lectures = glucoseRepository.getLast7DaysLectures(pid)
                    .sortedBy { it.dateHeure }

                if (lectures.isEmpty()) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Aucune donnée glycémique disponible"
                    ) }
                    return@launch
                }

                val now = LocalDateTime.now()

                // Points historiques (dernières 12h)
                val recent = lectures.filter {
                    ChronoUnit.HOURS.between(it.dateHeure, now) <= 12
                }
                val historicalPoints = recent.map { lecture ->
                    val hoursAgo = ChronoUnit.MINUTES.between(lecture.dateHeure, now) / 60f
                    PredictivePoint(
                        hoursFromNow = -hoursAgo,
                        value = lecture.valeur,
                        isPrediction = false
                    )
                }

                // Prédiction simple basée sur la tendance
                val predictedPoints = generatePrediction(lectures)
                val currentValue = lectures.last().valeur

                // Analyser risques
                val (riskLevel, alerts) = analyzeRisks(currentValue, predictedPoints)

                // Description tendance
                val trend = glucoseRepository.analyzeTrend(lectures.reversed())
                val trendDesc = when (trend) {
                    GlucoseRepository.TrendResult.RISING -> "Tendance à la hausse"
                    GlucoseRepository.TrendResult.FALLING -> "Tendance à la baisse"
                    GlucoseRepository.TrendResult.STABLE -> "Glycémie stable"
                }

                val predictedValues = predictedPoints.map { it.value }
                _uiState.update { it.copy(
                    isLoading = false,
                    historicalPoints = historicalPoints,
                    predictedPoints = predictedPoints,
                    currentValue = currentValue,
                    predictedMin = predictedValues.minOrNull(),
                    predictedMax = predictedValues.maxOrNull(),
                    trendDescription = trendDesc,
                    riskLevel = riskLevel,
                    alerts = alerts
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Génère des points de prédiction pour les 6 prochaines heures
     * en utilisant une régression linéaire pondérée sur les lectures récentes
     */
    private fun generatePrediction(lectures: List<LectureGlucoseEntity>): List<PredictivePoint> {
        if (lectures.size < 3) return emptyList()

        val now = LocalDateTime.now()
        val recentLectures = lectures.takeLast(10)

        // Calcul de la tendance par régression linéaire
        val points = recentLectures.mapIndexed { i, l ->
            val hours = ChronoUnit.MINUTES.between(l.dateHeure, now).toDouble() / 60.0
            Pair(-hours, l.valeur)
        }

        val n = points.size.toDouble()
        val sumX = points.sumOf { it.first }
        val sumY = points.sumOf { it.second }
        val sumXY = points.sumOf { it.first * it.second }
        val sumX2 = points.sumOf { it.first * it.first }

        val slope = if (n * sumX2 - sumX * sumX != 0.0) {
            (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        } else 0.0

        val intercept = (sumY - slope * sumX) / n
        val currentEstimate = intercept // At x=0 (now)

        // Générer des points prédits toutes les 30 minutes pour 6 heures
        val predictions = mutableListOf<PredictivePoint>()
        for (i in 1..12) {
            val hoursAhead = i * 0.5f
            var predicted = currentEstimate + slope * hoursAhead

            // Appliquer une atténuation vers la moyenne (mean reversion)
            val meanGlucose = lectures.takeLast(20).map { it.valeur }.average()
            val reversionFactor = 0.05 * hoursAhead // Plus on va loin, plus on revient vers la moyenne
            predicted = predicted * (1 - reversionFactor) + meanGlucose * reversionFactor

            // Borner les valeurs entre 40 et 400
            predicted = predicted.coerceIn(40.0, 400.0)

            // Confiance décroissante avec le temps
            val confidence = (1f - hoursAhead / 8f).coerceIn(0.2f, 1f)

            predictions.add(PredictivePoint(
                hoursFromNow = hoursAhead,
                value = predicted,
                isPrediction = true,
                confidence = confidence
            ))
        }

        return predictions
    }

    private fun analyzeRisks(
        currentValue: Double,
        predictions: List<PredictivePoint>
    ): Pair<RiskLevel, List<String>> {
        val alerts = mutableListOf<String>()

        // Vérifier la valeur actuelle
        when {
            currentValue < 54 -> alerts.add("URGENCE: Hypoglycémie sévère (${currentValue.toInt()} mg/dL)")
            currentValue < 70 -> alerts.add("Hypoglycémie détectée (${currentValue.toInt()} mg/dL)")
            currentValue > 300 -> alerts.add("Hyperglycémie sévère (${currentValue.toInt()} mg/dL)")
            currentValue > 180 -> alerts.add("Hyperglycémie (${currentValue.toInt()} mg/dL)")
        }

        // Vérifier les prédictions
        val predictedMin = predictions.minOfOrNull { it.value } ?: currentValue
        val predictedMax = predictions.maxOfOrNull { it.value } ?: currentValue

        if (predictedMin < 70) {
            alerts.add("Risque d'hypoglycémie dans les prochaines heures")
        }
        if (predictedMax > 250) {
            alerts.add("Risque d'hyperglycémie dans les prochaines heures")
        }

        val riskLevel = when {
            currentValue < 54 || currentValue > 300 -> RiskLevel.CRITICAL
            currentValue < 70 || currentValue > 250 -> RiskLevel.HIGH
            predictedMin < 70 || predictedMax > 250 -> RiskLevel.MODERATE
            currentValue in 70.0..180.0 && predictedMin >= 60 && predictedMax <= 200 -> RiskLevel.LOW
            else -> RiskLevel.NORMAL
        }

        return Pair(riskLevel, alerts)
    }

    fun requestRollyAnalysis() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isAnalyzing = true) }

                val pid = if (patientId > 0) patientId else {
                    patientRepository.getAllPatientsList().firstOrNull()?.id ?: return@launch
                }

                val patient = patientRepository.getPatientById(pid) ?: return@launch
                val lectures = glucoseRepository.getLast7DaysLectures(pid)
                val latestHbA1c = glucoseRepository.getLatestHbA1c(pid)

                val stats = glucoseRepository.getStatistics(pid, 7)
                val hba1cEstimee = if (stats.totalLectures >= 10) {
                    com.diabeto.data.entity.HbA1cEntity.estimerDepuisGlycemieMoyenne(stats.moyenne)
                } else null

                val analysis = chatbotRepository.analysePredictive7Jours(
                    patient, lectures, latestHbA1c, hba1cEstimee
                )

                _uiState.update { it.copy(
                    isAnalyzing = false,
                    rollyAnalysis = analysis
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isAnalyzing = false,
                    error = "Erreur d'analyse: ${e.message}"
                ) }
            }
        }
    }

    fun dismissAnalysis() {
        _uiState.update { it.copy(rollyAnalysis = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
