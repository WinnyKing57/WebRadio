package com.example.webradioapp.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
// Fragment import might not be needed if not directly used after removing the listener.
// import androidx.fragment.app.Fragment
// Explicit import for SharedPreferencesManager
import com.example.webradioapp.utils.SharedPreferencesManager
import androidx.navigation.fragment.NavHostFragment // Added for NavHostFragment
import androidx.navigation.ui.setupWithNavController // Added for setupWithNavController
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.example.webradioapp.R // Already present, ensure it's used correctly
// Removed duplicate imports, ensured necessary ones are present
import com.example.webradioapp.services.StreamingService
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import com.example.webradioapp.viewmodels.FavoritesViewModel // Added
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * The main activity of the application.
 * This activity hosts the bottom navigation bar and manages the display of different content fragments.
 * It also handles the application of the selected accent color theme and initializes the Google Cast context.
 */
class MainActivity : AppCompatActivity(), SleepTimerDialogFragment.SleepTimerDialogListener {

    // Old Mini-Player views (to be removed or repurposed if IDs are identical)
    // private lateinit var miniPlayerContainer: View // This ID might be different now (new: mini_player_view_container)
    // private lateinit var ivMiniPlayerIcon: ImageView // Old ID: iv_mini_player_icon
    // private lateinit var tvMiniPlayerStationName: TextView // Old ID: tv_mini_player_station_name
    // private lateinit var ibMiniPlayerPlayPause: ImageButton // Old ID: ib_mini_player_play_pause
    // private lateinit var ibMiniPlayerSleepTimer: ImageButton // Old ID: ib_mini_player_sleep_timer

    // New Mini-Player (from layout_bottom_mini_player.xml)
    private lateinit var newMiniPlayerViewContainer: View // ID: mini_player_view_container (the FrameLayout)
    private lateinit var ivNewMiniPlayerStationIcon: ImageView // ID: iv_mini_player_station_icon
    private lateinit var tvNewMiniPlayerStationName: TextView // ID: tv_mini_player_station_name
    private lateinit var ibNewMiniPlayerPlayPause: ImageButton // ID: ib_mini_player_play_pause

    // Full Player (from layout_full_player_bottom_sheet.xml)
    private lateinit var fullPlayerBottomSheetView: View // ID: full_player_bottom_sheet (the FrameLayout acting as bottom sheet)
    private lateinit var ivFullPlayerStationArtwork: ImageView // ID: iv_full_player_station_artwork
    private lateinit var tvFullPlayerStationName: TextView // ID: tv_full_player_station_name
    private lateinit var tvFullPlayerStationDetails: TextView // ID: tv_full_player_station_details
    private lateinit var ibFullPlayerPlayPause: ImageButton // ID: ib_full_player_play_pause
    private lateinit var ibFullPlayerFavorite: ImageButton // ID: ib_full_player_favorite
    private lateinit var seekbarFullPlayerVolume: SeekBar // ID: seekbar_full_player_volume
    // Removed ibMiniPlayerSleepTimer as its functionality is not being re-added in this step

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var audioManager: AudioManager

    private val stationViewModel: StationViewModel by viewModels() // Ensured ViewModel is present
    private val favoritesViewModel: FavoritesViewModel by viewModels() // Added for favorite status

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

        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)

        // Initialize new Mini Player views
        newMiniPlayerViewContainer = findViewById(R.id.mini_player_view_container)
        ivNewMiniPlayerStationIcon = newMiniPlayerViewContainer.findViewById(R.id.iv_mini_player_station_icon)
        tvNewMiniPlayerStationName = newMiniPlayerViewContainer.findViewById(R.id.tv_mini_player_station_name)
        ibNewMiniPlayerPlayPause = newMiniPlayerViewContainer.findViewById(R.id.ib_mini_player_play_pause)

        // Initialize Full Player (BottomSheet) views
        fullPlayerBottomSheetView = findViewById(R.id.full_player_bottom_sheet)
        ivFullPlayerStationArtwork = fullPlayerBottomSheetView.findViewById(R.id.iv_full_player_station_artwork)
        tvFullPlayerStationName = fullPlayerBottomSheetView.findViewById(R.id.tv_full_player_station_name)
        tvFullPlayerStationDetails = fullPlayerBottomSheetView.findViewById(R.id.tv_full_player_station_details)
        ibFullPlayerPlayPause = fullPlayerBottomSheetView.findViewById(R.id.ib_full_player_play_pause)
        ibFullPlayerFavorite = fullPlayerBottomSheetView.findViewById(R.id.ib_full_player_favorite)
        seekbarFullPlayerVolume = fullPlayerBottomSheetView.findViewById(R.id.seekbar_full_player_volume)

        // Setup BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(fullPlayerBottomSheetView)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 0 // Mini player is separate

        setupNewPlayerControls()
        setupBottomSheetCallback()
        setupNewLiveDataObservers()
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
        // NAV_GRAPH_CACHE_BUST_001
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
                Intent(this@MainActivity, StreamingService::class.java).also { intent ->
                    intent.action = StreamingService.ACTION_STOP
                    startService(intent)
                }
                // Optionally, update UI to show timer finished or hide timer indication
                Toast.makeText(this@MainActivity, "Sleep timer finished", Toast.LENGTH_SHORT).show()
            }
        }.start()
        Toast.makeText(this, "Sleep timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
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
            val currentIsPlaying = StreamingService.isPlayingLiveData.value ?: false
            val action = if (currentIsPlaying) StreamingService.ACTION_PAUSE else StreamingService.ACTION_PLAY
            Intent(this, StreamingService::class.java).also { intentValue ->
                intentValue.action = action
                // If station is null but user clicks play (e.g. on mini player that was visible),
                // the service should handle this (e.g. play last station or do nothing)
                // For now, we assume currentPlayingStationLiveData.value is the source of truth if action is PLAY
                if (action == StreamingService.ACTION_PLAY && StreamingService.currentPlayingStationLiveData.value == null) {
                    // Optionally handle case where play is hit with no station (e.g. show message)
                    // For now, service will handle this (or not, if no station info passed)
                } else if (StreamingService.currentPlayingStationLiveData.value != null) {
                     intentValue.putExtra(StreamingService.EXTRA_STATION_OBJECT, StreamingService.currentPlayingStationLiveData.value)
                }
                startService(intentValue)
            }
        }
        ibNewMiniPlayerPlayPause.setOnClickListener(playPauseClickListener)
        ibFullPlayerPlayPause.setOnClickListener(playPauseClickListener)

        ibFullPlayerFavorite.setOnClickListener {
            StreamingService.currentPlayingStationLiveData.value?.let { station ->
                stationViewModel.toggleFavoriteStatus(station) // Assuming stationViewModel is available
            }
        }
    }

    private fun setupBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        newMiniPlayerViewContainer.visibility = View.GONE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN -> {
                        if (StreamingService.currentPlayingStationLiveData.value != null) {
                            newMiniPlayerViewContainer.visibility = View.VISIBLE
                        } else {
                            newMiniPlayerViewContainer.visibility = View.GONE
                        }
                    }
                    // Other states can be handled if needed (DRAGGING, SETTLING, HALF_EXPANDED)
                    else -> { /* No-op */ }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Example: Fade out mini player as bottom sheet slides up
                // newMiniPlayerViewContainer.alpha = 1.0f - slideOffset
            }
        })
    }

    private fun setupNewLiveDataObservers() {
        StreamingService.isPlayingLiveData.observe(this) { isPlaying ->
            val playPauseRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
            ibNewMiniPlayerPlayPause.setImageResource(playPauseRes)
            ibFullPlayerPlayPause.setImageResource(playPauseRes)
        }

        StreamingService.currentPlayingStationLiveData.observe(this) { station ->
            if (station != null) {
                tvNewMiniPlayerStationName.text = station.name
                tvFullPlayerStationName.text = station.name
                tvFullPlayerStationDetails.text = "${station.genre ?: ""} - ${station.country ?: ""}" // Example detail

                Glide.with(this@MainActivity)
                    .load(station.favicon)
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .error(R.drawable.ic_radio_placeholder)
                    .into(ivNewMiniPlayerStationIcon)

                Glide.with(this@MainActivity)
                    .load(station.favicon.ifEmpty { station.favicon }) // Use favicon, or a larger image URL if available
                    .placeholder(R.drawable.ic_radio_placeholder) // Larger placeholder for artwork
                    .error(R.drawable.ic_radio_placeholder)
                    .into(ivFullPlayerStationArtwork)

                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                     newMiniPlayerViewContainer.visibility = View.VISIBLE
                }
                // Favorite status for full player button will be handled by observeFavoriteChanges
            } else {
                newMiniPlayerViewContainer.visibility = View.GONE
                if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                tvNewMiniPlayerStationName.text = ""
                tvFullPlayerStationName.text = "Nothing Playing"
                tvFullPlayerStationDetails.text = ""
                ivNewMiniPlayerStationIcon.setImageResource(R.drawable.ic_radio_placeholder)
                ivFullPlayerStationArtwork.setImageResource(R.drawable.ic_radio_placeholder)
            }
        }

        StreamingService.playerErrorLiveData.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Potentially hide bottom sheet or show error state within it
                // StreamingService.playerErrorLiveData.postValue(null) // Clear error after showing
            }
        }
    }

    private fun observeFavoriteChanges() {
        favoritesViewModel.favoriteStations.observe(this) { favoritesList ->
            val currentStation = StreamingService.currentPlayingStationLiveData.value
            currentStation?.let { station ->
                val isFavorite = favoritesList.any { it.id == station.id }
                ibFullPlayerFavorite.setImageResource(
                    if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_border
                )
            }
        }
    }

    private fun setupVolumeControls() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekbarFullPlayerVolume.max = maxVolume
        seekbarFullPlayerVolume.progress = currentVolume

        seekbarFullPlayerVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
