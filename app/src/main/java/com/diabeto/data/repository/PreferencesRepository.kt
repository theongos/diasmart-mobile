package com.diabeto.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "diasmart_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val code: String, val displayName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية")
}

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MEDICATION_REMINDERS = booleanPreferencesKey("medication_reminders")
        val MEASUREMENT_REMINDERS = booleanPreferencesKey("measurement_reminders")
        val APPOINTMENT_REMINDERS = booleanPreferencesKey("appointment_reminders")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        try {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        try {
            AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.FRENCH.name)
        } catch (_: Exception) {
            AppLanguage.FRENCH
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val medicationReminders: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MEDICATION_REMINDERS] ?: true
    }

    val measurementReminders: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MEASUREMENT_REMINDERS] ?: true
    }

    val appointmentReminders: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.APPOINTMENT_REMINDERS] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setMedicationReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MEDICATION_REMINDERS] = enabled }
    }

    suspend fun setMeasurementReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MEASUREMENT_REMINDERS] = enabled }
    }

    suspend fun setAppointmentReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APPOINTMENT_REMINDERS] = enabled }
    }
}
