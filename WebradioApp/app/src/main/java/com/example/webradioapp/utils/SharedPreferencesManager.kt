package com.example.webradioapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.webradioapp.model.RadioStation // Assurez-vous que RadioStation est importé

/**
 * Utility class for managing SharedPreferences operations
 */
object SharedPreferencesManager {

    private const val PREFS_NAME = "WebRadioPrefs"
    const val ACCENT_THEME_DEFAULT = "Default"
    const val ACCENT_THEME_BLUE = "AccentBlue"
    const val ACCENT_THEME_GREEN = "AccentGreen"
    const val ACCENT_THEME_ORANGE = "AccentOrange"
    private const val KEY_ACCENT_COLOR = "accent_color_theme"

    // Keys for preferences
    private const val KEY_VOLUME = "volume"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_SLEEP_TIMER_DEFAULT = "sleep_timer_default"
    private const val KEY_LAST_STATION_URL = "last_station_url"
    private const val KEY_LAST_STATION_NAME = "last_station_name"
    private const val KEY_STATION_HISTORY = "station_history"
    private const val MAX_HISTORY_SIZE = 20 // Optionnel, pour limiter la taille

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveVolume(context: Context, volume: Float) {
        getPreferences(context).edit()
            .putFloat(KEY_VOLUME, volume)
            .apply()
    }

    fun getVolume(context: Context): Float {
        return getPreferences(context).getFloat(KEY_VOLUME, 0.5f)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setSleepTimerDefault(context: Context, minutes: Int) {
        getPreferences(context).edit()
            .putInt(KEY_SLEEP_TIMER_DEFAULT, minutes)
            .apply()
    }

    fun getSleepTimerDefault(context: Context): Int {
        return getPreferences(context).getInt(KEY_SLEEP_TIMER_DEFAULT, 30)
    }

    fun saveLastStation(context: Context, url: String, name: String) {
        getPreferences(context).edit()
            .putString(KEY_LAST_STATION_URL, url)
            .putString(KEY_LAST_STATION_NAME, name)
            .apply()
    }

    fun getLastStationUrl(context: Context): String? {
        return getPreferences(context).getString(KEY_LAST_STATION_URL, null)
    }

    fun getLastStationName(context: Context): String? {
        return getPreferences(context).getString(KEY_LAST_STATION_NAME, null)
    }

    fun getAccentColorTheme(context: Context): String {
        return getPreferences(context).getString(KEY_ACCENT_COLOR, ACCENT_THEME_DEFAULT) ?: ACCENT_THEME_DEFAULT
    }

    fun setAccentColorTheme(context: Context, themeName: String) {
        getPreferences(context).edit()
            .putString(KEY_ACCENT_COLOR, themeName)
            .apply()
    }

    fun getStationHistory(context: Context): List<RadioStation> {
        val prefs = getPreferences(context)
        val jsonHistory = prefs.getString(KEY_STATION_HISTORY, null)
        if (jsonHistory == null) {
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<RadioStation>>() {}.type
            Gson().fromJson(jsonHistory, type) ?: emptyList()
        } catch (e: Exception) {
            // Log error or handle corruption
            emptyList()
        }
    }

    fun addStationToHistory(context: Context, station: RadioStation) {
        val history = getStationHistory(context).toMutableList()
        // Éviter les doublons basé sur l'ID, en gardant le plus récent
        history.removeAll { it.id == station.id }
        history.add(0, station) // Ajoute au début
        // Limiter la taille
        val trimmedHistory = if (history.size > MAX_HISTORY_SIZE) history.subList(0, MAX_HISTORY_SIZE) else history

        val jsonHistory = Gson().toJson(trimmedHistory)
        getPreferences(context).edit().putString(KEY_STATION_HISTORY, jsonHistory).apply()
    }
}