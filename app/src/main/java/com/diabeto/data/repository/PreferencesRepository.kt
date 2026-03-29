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
enum class GlucoseUnit(val label: String, val shortLabel: String) {
    MG_DL("mg/dL", "mg/dL"),
    MMOL_L("mmol/L", "mmol/L");

    fun convert(valueInMgDl: Double): Double = when (this) {
        MG_DL -> valueInMgDl
        MMOL_L -> valueInMgDl / 18.0182
    }

    fun format(valueInMgDl: Double): String = when (this) {
        MG_DL -> "${valueInMgDl.toInt()}"
        MMOL_L -> "%.1f".format(valueInMgDl / 18.0182)
    }
}
enum class MeasureType(val displayName: String) {
    CAPILLARY("Capillaire (doigt)"),
    CGM("CGM (capteur continu)"),
    VENOUS("Veineux (laboratoire)")
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
        val GLUCOSE_UNIT = stringPreferencesKey("glucose_unit")
        val MEASURE_TYPE = stringPreferencesKey("measure_type")
        val TARGET_MIN = stringPreferencesKey("glycemic_target_min")
        val TARGET_MAX = stringPreferencesKey("glycemic_target_max")
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

    val glucoseUnit: Flow<GlucoseUnit> = context.dataStore.data.map { prefs ->
        try {
            GlucoseUnit.valueOf(prefs[Keys.GLUCOSE_UNIT] ?: GlucoseUnit.MG_DL.name)
        } catch (_: Exception) { GlucoseUnit.MG_DL }
    }

    val measureType: Flow<MeasureType> = context.dataStore.data.map { prefs ->
        try {
            MeasureType.valueOf(prefs[Keys.MEASURE_TYPE] ?: MeasureType.CAPILLARY.name)
        } catch (_: Exception) { MeasureType.CAPILLARY }
    }

    val targetMin: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.TARGET_MIN]?.toDoubleOrNull() ?: 70.0
    }

    val targetMax: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.TARGET_MAX]?.toDoubleOrNull() ?: 180.0
    }

    suspend fun setGlucoseUnit(unit: GlucoseUnit) {
        context.dataStore.edit { it[Keys.GLUCOSE_UNIT] = unit.name }
    }

    suspend fun setMeasureType(type: MeasureType) {
        context.dataStore.edit { it[Keys.MEASURE_TYPE] = type.name }
    }

    suspend fun setGlycemicTarget(min: Double, max: Double) {
        context.dataStore.edit {
            it[Keys.TARGET_MIN] = min.toString()
            it[Keys.TARGET_MAX] = max.toString()
        }
    }
}
