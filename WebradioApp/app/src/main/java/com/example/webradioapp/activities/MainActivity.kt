package com.example.webradioapp.activities

import android.content.Context // Added
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.activity.viewModels // Added
import androidx.appcompat.app.AppCompatActivity
// Fragment import might not be needed if not directly used after removing the listener.
// import androidx.fragment.app.Fragment
// Explicit import for SharedPreferencesManager
import com.example.webradioapp.db.AppDatabase // Added
import com.example.webradioapp.db.StationRepository // Added
import com.example.webradioapp.network.ApiClient // Added
import com.example.webradioapp.utils.SharedPreferencesManager
import androidx.navigation.fragment.NavHostFragment // Added for NavHostFragment
import androidx.navigation.ui.setupWithNavController // Added for setupWithNavController
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.example.webradioapp.R // Already present, ensure it's used correctly
// Removed duplicate imports, ensured necessary ones are present
import com.example.webradioapp.services.StreamingService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.content.Intent
import android.media.AudioManager
import android.os.CountDownTimer
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider // Added for ViewModel
import com.bumptech.glide.Glide
import com.example.webradioapp.dialogs.SleepTimerDialogFragment
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.viewmodels.StationViewModel // Added
import com.example.webradioapp.viewmodels.StationViewModelFactory // Added
import com.example.webradioapp.viewmodels.FavoritesViewModel // Added
import com.example.webradioapp.viewmodels.FavoritesViewModelFactory // Added
import com.example.webradioapp.viewmodels.PlaybackViewModel // Added
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.lifecycle.Observer // Added for explicit Observer

/**
 * The main activity of the application.
 * This activity hosts the bottom navigation bar and manages the display of different content fragments.
 * It also handles the application of the selected accent color theme and initializes the Google Cast context.
 */
class MainActivity : AppCompatActivity(), SleepTimerDialogFragment.SleepTimerDialogListener {

    // New Mini-Player (from layout_bottom_mini_player.xml)
    private lateinit var newMiniPlayerViewContainer: View // ID: mini_player_view_container (the FrameLayout)
    private lateinit var ivNewMiniPlayerStationIcon: ImageView // ID: iv_mini_player_station_icon
    private lateinit var tvNewMiniPlayerStationName: TextView // ID: tv_mini_player_station_name
    private lateinit var ibNewMiniPlayerPlayPause: ImageButton // ID: ib_mini_player_play_pause

    // Full Player (from layout_full_player_bottom_sheet.xml)
    private lateinit var fullPlayerBottomSheetView: View // ID: full_player_bottom_sheet (the FrameLayout acting as bottom sheet)
    private lateinit var ivFullPlayerStationArtwork: ImageView // ID: iv_full_player_station_artwork
    private lateinit var tvFullPlayerStationName: TextView // ID: tv_full_player_station_name
    private lateinit var tvFullPlayerSongTitle: TextView
    private lateinit var tvFullPlayerArtistName: TextView
    private lateinit var ibFullPlayerPlayPause: ImageButton // ID: ib_full_player_play_pause
    private lateinit var ibFullPlayerFavorite: ImageButton // ID: ib_full_player_favorite
    private lateinit var seekbarVolume: SeekBar // ID: seekbar_volume
    // Removed ibMiniPlayerSleepTimer as its functionality is not being re-added in this step

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var audioManager: AudioManager

    // private val stationViewModel: StationViewModel by viewModels() // Ensured ViewModel is present
    // private val favoritesViewModel: FavoritesViewModel by viewModels() // Added for favorite status
    private lateinit var stationViewModel: StationViewModel
    private lateinit var favoritesViewModel: FavoritesViewModel
    private val playbackViewModel: PlaybackViewModel by viewModels()

    private var sleepTimer: CountDownTimer? = null

    // Removed onNavigationItemSelectedListener

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

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager // Added

        // ViewModel Initialization with Factories
        val application = requireNotNull(this).application
        val database = AppDatabase.getDatabase(application)
        val apiClientInstance = ApiClient.instance // Ensure ApiClient is imported
        val apiService: com.example.webradioapp.network.RadioBrowserApiService = apiClientInstance // Explicit typing

        val stationRepository = com.example.webradioapp.db.StationRepository( // Use fully qualified name for clarity
            application, // Or applicationContext if application is not the direct Application instance
            database.favoriteStationDao(),
            database.historyStationDao(),
            database.countryDao(),
            database.genreDao(),
            database.languageDao(),
            apiService // Pass the new explicitly typed variable
        )

        val stationViewModelFactory = StationViewModelFactory(application, stationRepository)
        stationViewModel = ViewModelProvider(this, stationViewModelFactory).get(StationViewModel::class.java)

        val favoritesViewModelFactory = FavoritesViewModelFactory(application, stationRepository)
        favoritesViewModel = ViewModelProvider(this, favoritesViewModelFactory).get(com.example.webradioapp.viewmodels.FavoritesViewModel::class.java) // Ensure fully qualified name for FavoritesViewModel

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavView = findViewById<BottomNavigationView?>(R.id.bottom_navigation)
        val navigationRailView = findViewById<NavigationRailView?>(R.id.navigation_rail)

        if (bottomNavView != null) {
            bottomNavView.setupWithNavController(navController)
            Log.d("MainActivity", "BottomNavigationView setup complete.")
        } else if (navigationRailView != null) {
            navigationRailView.setupWithNavController(navController)
            Log.d("MainActivity", "NavigationRailView setup complete.")
        } else {
            Log.e("MainActivity", "Neither BottomNavigationView nor NavigationRailView found. Navigation will not work.")
            // Potentially throw an exception or handle this error state more gracefully
            // For now, logging an error is the primary action.
        }

        // Initialize new Mini Player views
        newMiniPlayerViewContainer = findViewById(R.id.mini_player_view_container)
        ivNewMiniPlayerStationIcon = newMiniPlayerViewContainer.findViewById(R.id.iv_mini_player_album_art)
        tvNewMiniPlayerStationName = newMiniPlayerViewContainer.findViewById(R.id.tv_mini_player_station_name)
        ibNewMiniPlayerPlayPause = newMiniPlayerViewContainer.findViewById(R.id.ib_mini_player_play_pause)

        // Initialize Full Player (BottomSheet) views
        fullPlayerBottomSheetView = findViewById(R.id.full_player_bottom_sheet)
        ivFullPlayerStationArtwork = fullPlayerBottomSheetView.findViewById(R.id.iv_full_player_album_art)
        tvFullPlayerStationName = fullPlayerBottomSheetView.findViewById(R.id.tv_full_player_station_name)
        tvFullPlayerSongTitle = fullPlayerBottomSheetView.findViewById(R.id.tv_full_player_song_title)
        tvFullPlayerArtistName = fullPlayerBottomSheetView.findViewById(R.id.tv_full_player_artist_name)
        ibFullPlayerPlayPause = fullPlayerBottomSheetView.findViewById(R.id.ib_full_player_play_pause)
        ibFullPlayerFavorite = fullPlayerBottomSheetView.findViewById(R.id.ib_full_player_favorite)
        seekbarVolume = fullPlayerBottomSheetView.findViewById(R.id.seekbar_volume)

        // Setup BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(fullPlayerBottomSheetView)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 0 // Mini player is separate

        setupNewPlayerControls()
        setupBottomSheetCallback()
        // setupNewLiveDataObservers() // Will be replaced by setupPlaybackObservers
        setupPlaybackObservers() // New method for PlaybackViewModel observers
        setupVolumeControls()
        observeFavoriteChanges() // Added


        // Initialize CastContext
        // It's important that this is called, but it may throw an IllegalStateException
        // if Google Play services is not available. Proper error handling should be added in a real app.
        try {
            CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            // Log error or inform user that Cast is unavailable
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        return true
    }

    override fun onTimerSet(minutes: Int) {
        sleepTimer?.cancel() // Cancel any existing timer

        sleepTimer = object : CountDownTimer(minutes * 60 * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Optionally, update UI with remaining time
                Log.d("MainActivity", "Sleep timer: ${millisUntilFinished / 1000}s remaining")
            }

            override fun onFinish() {
                Log.d("MainActivity", "Sleep timer finished. Stopping playback.")
                playbackViewModel.stopPlayback()
                // Optionally, update UI to show timer finished or hide timer indication
                Toast.makeText(this@MainActivity, getString(R.string.toast_sleep_timer_finished), Toast.LENGTH_SHORT).show()
            }
        }.start()
        Toast.makeText(this, getString(R.string.toast_sleep_timer_set, minutes), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        sleepTimer?.cancel() // Ensure timer is cancelled when activity is destroyed
    }

    private fun setupNewPlayerControls() {
        newMiniPlayerViewContainer.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val playPauseClickListener = View.OnClickListener {
            val currentIsPlaying = playbackViewModel.isPlaying.value ?: false
            if (currentIsPlaying) {
                playbackViewModel.pausePlayback()
            } else {
                // Ensure there's a station to play. This logic might need adjustment.
                // Perhaps get the last played station or current selection from another ViewModel.
                // For now, assuming currentPlayingStation holds what should be played if paused.
                playbackViewModel.currentPlayingStation.value?.let { stationToPlay ->
                    playbackViewModel.playStation(stationToPlay)
                } ?: run {
                    // Handle case: play pressed but no station is selected/available.
                    // Maybe pick from history or a default station.
                    // For now, log or show a Toast.
                    Log.w("MainActivity", "Play pressed but no current station in PlaybackViewModel.")
                    Toast.makeText(this, getString(R.string.toast_select_station_to_play), Toast.LENGTH_SHORT).show()
                }
            }
        }
        ibNewMiniPlayerPlayPause.setOnClickListener(playPauseClickListener)
        ibFullPlayerPlayPause.setOnClickListener(playPauseClickListener)

        ibFullPlayerFavorite.setOnClickListener {
            playbackViewModel.currentPlayingStation.value?.let { station ->
                stationViewModel.toggleFavoriteStatus(station) // Call on stationViewModel
            }
        }
    }

    private fun setupBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Visibility is now handled by updatePlayerUIVisibility,
                // which is called by the currentPlayingStation observer and this callback.
                updatePlayerUIVisibility(playbackViewModel.currentPlayingStation.value)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Optional: Fade out mini player as bottom sheet slides up
                // This can be refined later if desired.
                // if (newMiniPlayerViewContainer.visibility == View.VISIBLE && slideOffset > 0) {
                //     newMiniPlayerViewContainer.alpha = 1.0f - slideOffset
                // } else if (slideOffset == 0f) {
                //    newMiniPlayerViewContainer.alpha = 1.0f
                // }
            }
        })
    }

    private fun setupPlaybackObservers() {
        playbackViewModel.isPlaying.observe(this) { isPlaying ->
            val playPauseRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
            ibNewMiniPlayerPlayPause.setImageResource(playPauseRes)
            ibFullPlayerPlayPause.setImageResource(playPauseRes)
        }

        playbackViewModel.currentPlayingStation.observe(this) { station ->
            updatePlayerUIVisibility(station) // Centralized logic call
            if (station != null) {
                tvNewMiniPlayerStationName.text = station.name
                tvNewMiniPlayerStationName.isSelected = true // For marquee

                tvFullPlayerStationName.text = station.name
                // Placeholders for song title and artist name, actual metadata handling would be more complex
                tvFullPlayerSongTitle.text = station.genre ?: getString(R.string.text_metadata_unavailable)
                tvFullPlayerArtistName.text = station.country ?: ""

                Glide.with(this@MainActivity)
                    .load(station.favicon)
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .error(R.drawable.ic_radio_placeholder)
                    .into(ivNewMiniPlayerStationIcon)

                Glide.with(this@MainActivity)
                    .load(station.favicon.ifEmpty { station.favicon }) // Consider a larger image if available
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .error(R.drawable.ic_radio_placeholder)
                    .into(ivFullPlayerStationArtwork)

                // Favorite button state update
                favoritesViewModel.favoriteStations.value?.let { favoritesList ->
                    val isFavorite = favoritesList.any { favStation -> favStation.id == station.id }
                    ibFullPlayerFavorite.setImageResource(
                        if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_border
                    )
                } ?: ibFullPlayerFavorite.setImageResource(R.drawable.ic_star_border)

            } else {
                // Clear UI when no station is playing
                tvNewMiniPlayerStationName.text = ""
                tvFullPlayerStationName.text = getString(R.string.text_nothing_playing)
                tvFullPlayerSongTitle.text = ""
                tvFullPlayerArtistName.text = ""
                ivNewMiniPlayerStationIcon.setImageResource(R.drawable.ic_radio_placeholder)
                ivFullPlayerStationArtwork.setImageResource(R.drawable.ic_radio_placeholder)
                ibFullPlayerFavorite.setImageResource(R.drawable.ic_star_border)
            }
        }

        playbackViewModel.playerError.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Optionally, tell ViewModel to clear the error after showing
                // playbackViewModel.clearError()
            }
        }
    }

    // New helper method to centralize visibility logic
    private fun updatePlayerUIVisibility(station: RadioStation?) {
        if (station != null) {
            // A station is active or being loaded
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                newMiniPlayerViewContainer.visibility = View.GONE
            } else {
                newMiniPlayerViewContainer.visibility = View.VISIBLE
                // Ensure marquee is active for mini player station name if it's visible
                tvNewMiniPlayerStationName.isSelected = true
            }
        } else {
            // No station is active
            newMiniPlayerViewContainer.visibility = View.GONE
            // If no station, and full player is not already hidden, hide it.
            // This prevents an empty full player from showing if it was previously expanded.
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    private fun observeFavoriteChanges() {
        favoritesViewModel.favoriteStations.observe(this, Observer { favoritesList ->
            val currentStation = playbackViewModel.currentPlayingStation.value // Use PlaybackViewModel
            currentStation?.let { station ->
                val isFavorite = favoritesList?.any { favStation -> favStation.id == station.id } ?: false
                ibFullPlayerFavorite.setImageResource(
                    if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_border
                )
            } ?: ibFullPlayerFavorite.setImageResource(R.drawable.ic_star_border) // Default if no station or favorites list is null
        })
    }


    private fun setupVolumeControls() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekbarVolume.max = maxVolume
        seekbarVolume.progress = currentVolume

        seekbarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
