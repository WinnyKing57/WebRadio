package com.example.webradioapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.webradioapp.model.RadioStation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages application preferences using SharedPreferences.
 * This class handles storing and retrieving user settings such as theme choice,
 * notification enablement, and accent color preferences.
 *
 * @param context The application context, used to access SharedPreferences.
 */
class SharedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson() // Retained in case of future complex object storage needs, though not currently used.

    companion object {
        private const val PREFS_NAME = "webradio_prefs"
        // Removed: KEY_FAVORITE_STATIONS
        // Removed: KEY_STATION_HISTORY
        // Removed: MAX_HISTORY_SIZE

        // Theme Preferences
        private const val KEY_THEME_PREFERENCE = "theme_preference"
        // Values for theme preference, corresponding to AppCompatDelegate.MODE_NIGHT_*
        const val THEME_LIGHT = 1 // AppCompatDelegate.MODE_NIGHT_NO
        const val THEME_DARK = 2  // AppCompatDelegate.MODE_NIGHT_YES
        const val THEME_SYSTEM_DEFAULT = -1 // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        // Notification Preferences
        private const val PREF_ENABLE_STATION_UPDATES_NOTIFICATIONS = "enable_station_updates_notifications"

        // Accent Color Theme Preference
        private const val PREF_ACCENT_COLOR_THEME_NAME = "accent_color_theme_name"
        const val ACCENT_THEME_DEFAULT = "Default" // Represents Theme.WebradioApp
        const val ACCENT_THEME_BLUE = "AccentBlue"
        const val ACCENT_THEME_GREEN = "AccentGreen"
        const val ACCENT_THEME_ORANGE = "AccentOrange"

    }

    /** Sets the user's preferred app theme (Light, Dark, System Default). */
    fun setThemePreference(themeMode: Int) {
        prefs.edit().putInt(KEY_THEME_PREFERENCE, themeMode).apply()
    }

    /** Gets the user's preferred app theme. Defaults to System Default. */
    fun getThemePreference(): Int {
        // Default to System Default if no preference is set
        return prefs.getInt(KEY_THEME_PREFERENCE, THEME_SYSTEM_DEFAULT)
    }

    /** Enables or disables station update notifications. */
    fun setStationUpdatesNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_ENABLE_STATION_UPDATES_NOTIFICATIONS, enabled).apply()
    }

    /** Checks if station update notifications are enabled. Defaults to true. */
    fun isStationUpdatesNotificationEnabled(): Boolean {
        // Default to true (enabled)
        return prefs.getBoolean(PREF_ENABLE_STATION_UPDATES_NOTIFICATIONS, true)
    }

    /** Sets the user's chosen accent color theme name. */
    fun setAccentColorTheme(themeName: String) {
        prefs.edit().putString(PREF_ACCENT_COLOR_THEME_NAME, themeName).apply()
    }

    /** Gets the user's chosen accent color theme name. Defaults to "Default". */
    fun getAccentColorTheme(): String {
        return prefs.getString(PREF_ACCENT_COLOR_THEME_NAME, ACCENT_THEME_DEFAULT) ?: ACCENT_THEME_DEFAULT
    }

    // Removed Favorite and History methods as they are now handled by Room DB
}
