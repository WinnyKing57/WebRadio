package com.example.webradioapp.util

import android.content.Context
import android.content.SharedPreferences

object SettingsUtils {

    private const val PREFS_NAME = "app_settings_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_STATION_UPDATES_NOTIFICATIONS = "station_updates_notifications_enabled"

    // Default theme might be "system", "light", "dark"
    // For simplicity, using integers: 0 = System, 1 = Light, 2 = Dark
    fun getThemePreference(context: Context): Int {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, 0) // Default to System
    }

    fun setThemePreference(context: Context, themeValue: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_THEME, themeValue)
        editor.apply()
    }

    fun isStationUpdatesNotificationEnabled(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STATION_UPDATES_NOTIFICATIONS, true) // Default to true
    }

    fun setStationUpdatesNotificationEnabled(context: Context, enabled: Boolean) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_STATION_UPDATES_NOTIFICATIONS, enabled)
        editor.apply()
    }
}
