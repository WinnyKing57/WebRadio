package com.example.webradioapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.webradioapp.utils.NotificationHelper
import com.example.webradioapp.util.SettingsUtils // Added import

class WebradioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SharedPreferencesManager
        // val sharedPrefsManager = SharedPreferencesManager(applicationContext) // Removed
        val themePreferenceValue = SettingsUtils.getThemePreference(applicationContext)
        // Ensure themePreference is a valid value for setDefaultNightMode
        val appCompatMode = when (themePreferenceValue) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(appCompatMode)

        // Create Notification Channel(s)
        NotificationHelper.createNotificationChannel(applicationContext)
    }
}
