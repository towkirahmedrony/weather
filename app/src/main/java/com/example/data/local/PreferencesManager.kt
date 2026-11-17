package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrecipitationUnit
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.ThemePreference
import com.example.domain.model.UserPreferences
import com.example.domain.model.WindSpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.atmosDataStore: DataStore<Preferences> by preferencesDataStore(name = "atmos_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_pref")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val PRECIP_UNIT = stringPreferencesKey("precip_unit")
        val LANGUAGE = stringPreferencesKey("app_language")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val RAIN_ALERTS = booleanPreferencesKey("rain_alerts")
        val SEVERE_ALERTS = booleanPreferencesKey("severe_alerts")
        val SYNC_HOURS = intPreferencesKey("sync_hours")
        val SELECTED_LOC_ID = stringPreferencesKey("selected_loc_id")
    }

    val preferencesFlow: Flow<UserPreferences> = context.atmosDataStore.data.map { prefs ->
        UserPreferences(
            themePreference = prefs[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            temperatureUnit = prefs[Keys.TEMP_UNIT]?.let { runCatching { TemperatureUnit.valueOf(it) }.getOrNull() }
                ?: TemperatureUnit.CELSIUS,
            windSpeedUnit = prefs[Keys.WIND_UNIT]?.let { runCatching { WindSpeedUnit.valueOf(it) }.getOrNull() }
                ?: WindSpeedUnit.KMH,
            precipitationUnit = prefs[Keys.PRECIP_UNIT]?.let { runCatching { PrecipitationUnit.valueOf(it) }.getOrNull() }
                ?: PrecipitationUnit.MM,
            language = prefs[Keys.LANGUAGE]?.let { code ->
                AppLanguage.entries.firstOrNull { it.code == code }
            } ?: AppLanguage.ENGLISH,
            notificationsEnabled = prefs[Keys.NOTIF_ENABLED] ?: true,
            rainAlertsEnabled = prefs[Keys.RAIN_ALERTS] ?: true,
            severeAlertsEnabled = prefs[Keys.SEVERE_ALERTS] ?: true,
            syncIntervalHours = prefs[Keys.SYNC_HOURS] ?: 3,
            selectedLocationId = prefs[Keys.SELECTED_LOC_ID] ?: "bd_dhaka"
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.atmosDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.atmosDataStore.edit { it[Keys.TEMP_UNIT] = unit.name }
    }

    suspend fun setWindSpeedUnit(unit: WindSpeedUnit) {
        context.atmosDataStore.edit { it[Keys.WIND_UNIT] = unit.name }
    }

    suspend fun setPrecipitationUnit(unit: PrecipitationUnit) {
        context.atmosDataStore.edit { it[Keys.PRECIP_UNIT] = unit.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.atmosDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.NOTIF_ENABLED] = enabled }
    }

    suspend fun setRainAlertsEnabled(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.RAIN_ALERTS] = enabled }
    }

    suspend fun setSevereAlertsEnabled(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.SEVERE_ALERTS] = enabled }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        context.atmosDataStore.edit { it[Keys.SYNC_HOURS] = hours }
    }

    suspend fun setSelectedLocationId(id: String) {
        context.atmosDataStore.edit { it[Keys.SELECTED_LOC_ID] = id }
    }
}
