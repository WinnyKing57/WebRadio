package com.example.webradioapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.webradioapp.utils.NotificationHelper
import com.example.webradioapp.utils.SharedPreferencesManager // Added import

class WebradioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SharedPreferencesManager
        // val sharedPrefsManager = SharedPreferencesManager(applicationContext) // Removed
        val themePreferenceValue = SharedPreferencesManager.getNightModePreference(applicationContext)
        // Ensure themePreference is a valid value for setDefaultNightMode
        val appCompatMode = when (themePreferenceValue) {
            SharedPreferencesManager.NIGHT_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SharedPreferencesManager.NIGHT_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // Default to system for NIGHT_MODE_SYSTEM or any other value
        }
        AppCompatDelegate.setDefaultNightMode(appCompatMode)

        // Create Notification Channel(s)
        NotificationHelper.createNotificationChannel(applicationContext)
    }
}
