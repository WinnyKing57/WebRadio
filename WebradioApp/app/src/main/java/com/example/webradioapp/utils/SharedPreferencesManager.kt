package com.example.webradioapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.webradioapp.model.RadioStation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "webradio_prefs"
        private const val KEY_FAVORITE_STATIONS = "favorite_stations"
        private const val KEY_STATION_HISTORY = "station_history"
        private const val MAX_HISTORY_SIZE = 20

        // Theme Preferences
        private const val KEY_THEME_PREFERENCE = "theme_preference"
        // Values for theme preference, corresponding to AppCompatDelegate.MODE_NIGHT_*
        const val THEME_LIGHT = 1 // AppCompatDelegate.MODE_NIGHT_NO
        const val THEME_DARK = 2  // AppCompatDelegate.MODE_NIGHT_YES
        const val THEME_SYSTEM_DEFAULT = -1 // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    // Theme Preference
    fun setThemePreference(themeMode: Int) {
        prefs.edit().putInt(KEY_THEME_PREFERENCE, themeMode).apply()
    }

    fun getThemePreference(): Int {
        // Default to System Default if no preference is set
        return prefs.getInt(KEY_THEME_PREFERENCE, THEME_SYSTEM_DEFAULT)
    }


    // Favorites
    fun addFavorite(station: RadioStation) {
        val favorites = getFavoriteStations().toMutableList()
        if (!favorites.any { it.id == station.id }) {
            favorites.add(station)
            saveStationList(KEY_FAVORITE_STATIONS, favorites)
        }
    }

    fun removeFavorite(stationId: String) {
        val favorites = getFavoriteStations().toMutableList()
        favorites.removeAll { it.id == stationId }
        saveStationList(KEY_FAVORITE_STATIONS, favorites)
    }

    fun getFavoriteStations(): List<RadioStation> {
        return getStationList(KEY_FAVORITE_STATIONS)
    }

    fun isFavorite(stationId: String): Boolean {
        return getFavoriteStations().any { it.id == stationId }
    }

    // History
    fun addStationToHistory(station: RadioStation) {
        val history = getStationHistory().toMutableList()
        // Remove if already exists to move it to the top (most recent)
        history.removeAll { it.id == station.id }
        history.add(0, station) // Add to the beginning of the list

        // Trim history if it exceeds max size
        val trimmedHistory = if (history.size > MAX_HISTORY_SIZE) {
            history.subList(0, MAX_HISTORY_SIZE)
        } else {
            history
        }
        saveStationList(KEY_STATION_HISTORY, trimmedHistory)
    }

    fun getStationHistory(): List<RadioStation> {
        return getStationList(KEY_STATION_HISTORY)
    }

    // Generic methods for saving/retrieving lists
    private fun saveStationList(key: String, stations: List<RadioStation>) {
        val json = gson.toJson(stations)
        prefs.edit().putString(key, json).apply()
    }

    private fun getStationList(key: String): List<RadioStation> {
        val json = prefs.getString(key, null)
        return if (json != null) {
            val type = object : TypeToken<List<RadioStation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
}
