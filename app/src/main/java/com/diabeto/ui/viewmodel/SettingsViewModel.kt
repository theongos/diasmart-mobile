package com.diabeto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeto.data.repository.AppLanguage
import com.diabeto.data.repository.PreferencesRepository
import com.diabeto.data.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.FRENCH,
    val notificationsEnabled: Boolean = true,
    val medicationReminders: Boolean = true,
    val measurementReminders: Boolean = true,
    val appointmentReminders: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.language.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.notificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.medicationReminders.collect { enabled ->
                _uiState.update { it.copy(medicationReminders = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.measurementReminders.collect { enabled ->
                _uiState.update { it.copy(measurementReminders = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.appointmentReminders.collect { enabled ->
                _uiState.update { it.copy(appointmentReminders = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setMedicationReminders(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMedicationReminders(enabled) }
    }

    fun setMeasurementReminders(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMeasurementReminders(enabled) }
    }

    fun setAppointmentReminders(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAppointmentReminders(enabled) }
    }
}
