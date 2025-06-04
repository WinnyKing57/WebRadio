package com.example.webradioapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.webradioapp.utils.NotificationHelper
import com.example.webradioapp.utils.SharedPreferencesManager

class WebradioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SharedPreferencesManager
        val sharedPrefsManager = SharedPreferencesManager(applicationContext)
        val themePreference = sharedPrefsManager.getThemePreference()
        AppCompatDelegate.setDefaultNightMode(themePreference)

        // Create Notification Channel(s)
        NotificationHelper.createNotificationChannel(applicationContext)
    }
}
