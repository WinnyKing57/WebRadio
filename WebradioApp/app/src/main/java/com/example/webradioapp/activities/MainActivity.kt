package com.example.webradioapp.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
// Explicit import for SharedPreferencesManager
import com.example.webradioapp.utils.SharedPreferencesManager
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.example.webradioapp.R
import com.example.webradioapp.fragments.FavoritesFragment
import com.example.webradioapp.fragments.HomeFragment
import com.example.webradioapp.fragments.SearchFragment
import com.example.webradioapp.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * The main activity of the application.
 * This activity hosts the bottom navigation bar and manages the display of different content fragments.
 * It also handles the application of the selected accent color theme and initializes the Google Cast context.
 */
class MainActivity : AppCompatActivity() {

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        var selectedFragment: Fragment = HomeFragment()
        when (item.itemId) {
            R.id.nav_home -> selectedFragment = HomeFragment()
            R.id.nav_search -> selectedFragment = SearchFragment()
            R.id.nav_favorites -> selectedFragment = FavoritesFragment()
            R.id.nav_settings -> selectedFragment = SettingsFragment()
        }
        supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, selectedFragment).commit()
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply accent color theme from SharedPreferences BEFORE super.onCreate()
        // This ensures the theme is set before any views are inflated.
        val accentThemeName = SharedPreferencesManager.getAccentColorTheme(this) // Ligne corrigée
        // val accentThemeName = prefs.getAccentColorTheme() // Ligne supprimée
        if (accentThemeName != SharedPreferencesManager.ACCENT_THEME_DEFAULT) {
            try {
                // The theme name in styles.xml is "Theme.WebradioApp.AccentBlue", etc.
                // The stored name is "AccentBlue" (e.g., SharedPreferencesManager.ACCENT_THEME_BLUE).
                val fullThemeName = "Theme.WebradioApp.$accentThemeName"
                val themeResId = resources.getIdentifier(fullThemeName, "style", packageName)
                if (themeResId != 0) {
                    setTheme(themeResId)
                    Log.d("MainActivity", "Applied accent theme: $fullThemeName (ID: $themeResId)")
                } else {
                    Log.w("MainActivity", "Accent theme resource ID not found for $fullThemeName")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error applying accent theme $accentThemeName", e)
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        // Load HomeFragment by default
        // if (savedInstanceState == null) {
        //     supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
        // }

        // Initialize CastContext
        // It's important that this is called, but it may throw an IllegalStateException
        // if Google Play services is not available. Proper error handling should be added in a real app.
        try {
            CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            // Log error or inform user that Cast is unavailable
            e.printStackTrace()
        }
        // NAV_GRAPH_CACHE_BUST_001
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        return true
    }
}
