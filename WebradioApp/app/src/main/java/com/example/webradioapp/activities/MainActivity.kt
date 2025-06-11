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
import com.example.webradioapp.services.StreamingService
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import android.widget.Toast // Added

/**
 * The main activity of the application.
 * This activity hosts the bottom navigation bar and manages the display of different content fragments.
 * It also handles the application of the selected accent color theme and initializes the Google Cast context.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var miniPlayerContainer: View
    private lateinit var ivMiniPlayerIcon: ImageView
    private lateinit var tvMiniPlayerStationName: TextView
    private lateinit var ibMiniPlayerPlayPause: ImageButton

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

        try {
            Log.d("MainActivity", "Attempting Mini-Player setup and LiveData observation...")

            val includedMiniPlayerLayout = findViewById<View>(R.id.mini_player_layout_container)
            if (includedMiniPlayerLayout == null) {
                Log.e("MainActivity", "CRITICAL ERROR: mini_player_layout_container (the include tag itself) not found in activity_main.xml! Cannot proceed with mini-player setup.")
                throw IllegalStateException("Required view 'mini_player_layout_container' not found. Mini-player cannot be initialized.")
            }

            miniPlayerContainer = includedMiniPlayerLayout.findViewById(R.id.mini_player_container)
            ivMiniPlayerIcon = includedMiniPlayerLayout.findViewById(R.id.iv_mini_player_icon)
            tvMiniPlayerStationName = includedMiniPlayerLayout.findViewById(R.id.tv_mini_player_station_name)
            ibMiniPlayerPlayPause = includedMiniPlayerLayout.findViewById(R.id.ib_mini_player_play_pause)

            // Explicitly check if any of the essential views inside the mini-player are null
            if (miniPlayerContainer == null) Log.e("MainActivity", "Error: miniPlayerContainer is null after findViewById.")
            if (ivMiniPlayerIcon == null) Log.e("MainActivity", "Error: ivMiniPlayerIcon is null after findViewById.")
            if (tvMiniPlayerStationName == null) Log.e("MainActivity", "Error: tvMiniPlayerStationName is null after findViewById.")
            if (ibMiniPlayerPlayPause == null) Log.e("MainActivity", "Error: ibMiniPlayerPlayPause is null after findViewById.")

            // Throw if any crucial view is null to make the error obvious in logs if caught by outer try-catch
            if (miniPlayerContainer == null || ivMiniPlayerIcon == null || tvMiniPlayerStationName == null || ibMiniPlayerPlayPause == null) {
                throw IllegalStateException("One or more internal views of the mini-player are null after findViewById. Check IDs in mini_player.xml.")
            }

            StreamingService.isPlayingLiveData.observe(this) { isPlaying ->
                if (miniPlayerContainer.visibility == View.VISIBLE) {
                     ibMiniPlayerPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                }
            }

            StreamingService.currentPlayingStationLiveData.observe(this) { station ->
                if (station != null) {
                    tvMiniPlayerStationName.text = station.name
                    miniPlayerContainer.visibility = View.VISIBLE
                    val currentIsPlaying = StreamingService.isPlayingLiveData.value ?: false
                    ibMiniPlayerPlayPause.setImageResource(if (currentIsPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                } else {
                    miniPlayerContainer.visibility = View.GONE
                }
            }

            ibMiniPlayerPlayPause.setOnClickListener {
                val currentIsPlaying = StreamingService.isPlayingLiveData.value ?: false
                val action = if (currentIsPlaying) StreamingService.ACTION_PAUSE else StreamingService.ACTION_PLAY
                Intent(this, StreamingService::class.java).also { intentValue -> // Renamed to avoid conflict
                    intentValue.action = action
                    startService(intentValue)
                }
            }

            StreamingService.playerErrorLiveData.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
            }
            Log.d("MainActivity", "Mini-Player setup and LiveData observation part seems complete.")

        } catch (e: Exception) {
            Log.e("MainActivity", "CRITICAL ERROR DURING MINI-PLAYER SETUP or LiveData Observation in MainActivity.onCreate:", e)
        }

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
