package com.example.webradioapp.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class for managing SharedPreferences operations
 */
object SharedPreferencesManager {

    private const val PREFS_NAME = "WebRadioPrefs"

    // Keys for preferences
    private const val KEY_VOLUME = "volume"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_SLEEP_TIMER_DEFAULT = "sleep_timer_default"
    private const val KEY_LAST_STATION_URL = "last_station_url"
    private const val KEY_LAST_STATION_NAME = "last_station_name"

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
}