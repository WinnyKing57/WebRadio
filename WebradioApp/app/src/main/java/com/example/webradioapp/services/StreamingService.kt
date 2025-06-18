package com.example.webradioapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log // Ensure Log is imported for any new logging
import android.widget.Toast // Added import
import androidx.lifecycle.LiveData // Added
import androidx.lifecycle.MutableLiveData // Added
import androidx.core.app.NotificationCompat
import com.example.webradioapp.R
import com.example.webradioapp.activities.MainActivity
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.ApiClient // Added for ApiClient
import com.example.webradioapp.utils.SharedPreferencesManager
import androidx.media3.exoplayer.ExoPlayer // Changed
import androidx.media3.common.MediaItem // Changed
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException // Changed
import androidx.media3.common.Player // Changed
import androidx.media3.session.MediaSession
// Potentially add for PendingIntent if creating one for setSessionActivity:
// import android.app.PendingIntent
// import android.content.Intent
// import com.example.webradioapp.activities.MainActivity
import androidx.media3.cast.CastPlayer // Changed
import androidx.media3.cast.SessionAvailabilityListener // Changed
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

/**
 * Foreground service for handling audio streaming and Cast playback.
 * Manages ExoPlayer for local playback and CastPlayer for Chromecast sessions.
 * Handles audio focus, media notifications, sleep timer, and history logging.
 */
class StreamingService : Service(), AudioManager.OnAudioFocusChangeListener {

    private var localPlayer: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null // Made nullable
    private lateinit var castContext: CastContext // Remains lateinit, initialized in onCreate
    private lateinit var sessionManager: SessionManager
    private var currentSession: CastSession? = null

    private val binder = LocalBinder()
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedByAudioFocusLoss: Boolean = false
    // private lateinit var sharedPrefsManager: SharedPreferencesManager // Still used for Theme
    private lateinit var stationRepository: com.example.webradioapp.db.StationRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaSession: MediaSession? = null


    // Listener for Cast session events
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) { /* No-op */ }
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            currentSession = session
            switchToCastPlayer()
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) { /* No-op */ }
        override fun onSessionSuspended(session: CastSession, reason: Int) { /* No-op */ }
        override fun onSessionResuming(session: CastSession, sessionId: String) { /* No-op */ }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            currentSession = session
            switchToCastPlayer()
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) { /* No-op */ }
        override fun onSessionEnding(session: CastSession) { /* No-op */ }
        override fun onSessionEnded(session: CastSession, error: Int) {
            currentSession = null
            switchToLocalPlayer()
        }
    }

    // Listener for CastPlayer availability (alternative to SessionManagerListener for some cases)
    private val sessionAvailabilityListener = object : SessionAvailabilityListener {
        override fun onCastSessionAvailable() {
            switchToCastPlayer()
        }
        override fun onCastSessionUnavailable() {
            switchToLocalPlayer()
        }
    }


    private var activePlayer: Player? = null // Generic player interface to hold either local or cast
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerEndTimeMillis: Long = 0L

    // Instance LiveData
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPlayingStation = MutableLiveData<RadioStation?>()
    val currentPlayingStation: LiveData<RadioStation?> = _currentPlayingStation

    private val _playerError = MutableLiveData<String?>()
    val playerError: LiveData<String?> = _playerError


    companion object {
        const val ACTION_PLAY = "com.example.webradioapp.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.webradioapp.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.webradioapp.ACTION_STOP"
        const val ACTION_SET_SLEEP_TIMER = "com.example.webradioapp.ACTION_SET_SLEEP_TIMER"
        const val ACTION_CANCEL_SLEEP_TIMER = "com.example.webradioapp.ACTION_CANCEL_SLEEP_TIMER"
        const val EXTRA_STREAM_URL = "com.example.webradioapp.EXTRA_STREAM_URL"
        const val EXTRA_SLEEP_DURATION_MS = "com.example.webradioapp.EXTRA_SLEEP_DURATION_MS"
        const val EXTRA_STATION_OBJECT = "com.example.webradioapp.EXTRA_STATION_OBJECT" // For passing RadioStation
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "StreamingServiceChannel"
    }

    inner class LocalBinder : Binder() {
        fun getService(): StreamingService = this@StreamingService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // sharedPrefsManager = SharedPreferencesManager(applicationContext) // For theme

        val database = com.example.webradioapp.db.AppDatabase.getDatabase(applicationContext)
        val apiService = com.example.webradioapp.network.ApiClient.instance // Use .instance
        stationRepository = com.example.webradioapp.db.StationRepository(
            applicationContext,
            database.favoriteStationDao(),
            database.historyStationDao(),
            database.countryDao(),
            database.genreDao(),
            database.languageDao(),
            apiService
        )

        castContext = CastContext.getSharedInstance(this)
        sessionManager = castContext.sessionManager
        currentSession = sessionManager.currentCastSession

        initializeLocalPlayer()
        initializeCastPlayer()

        activePlayer = if (currentSession != null) castPlayer else localPlayer
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        // castPlayer.setSessionAvailabilityListener(sessionAvailabilityListener) // Alternative listener

        if (localPlayer != null) {
            // val sessionActivityPendingIntent = PendingIntent.getActivity(
            //     this,
            //     0,
            //     Intent(this, MainActivity::class.java),
            //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            // ) // Optional: Good for session to open UI

            mediaSession = MediaSession.Builder(this, localPlayer!!)
                // .setSessionActivity(sessionActivityPendingIntent) // Uncomment if using
                .build()
        } else {
            android.util.Log.e("StreamingService", "LocalPlayer is null, MediaSession cannot be initialized.")
        }

        createNotificationChannel()
    }

    private fun initializeLocalPlayer() {
        if (localPlayer == null) {
            localPlayer = ExoPlayer.Builder(this).build().apply {
                addListener(playerListener)
            }
        }
        // If not casting, set activePlayer to localPlayer initially
        // The sessionManagerListener in onCreate will handle switches later
        if (activePlayer == null && (castPlayer == null || castPlayer?.isCastSessionAvailable == false)) {
            activePlayer = localPlayer
        }
    }

    private fun initializeCastPlayer() {
        // castContext is initialized in onCreate. If getSharedInstance can fail, this might need more checks.
        // For now, assuming castContext will be valid if this method is called after its init.
        castPlayer = CastPlayer(this.castContext).apply {
            addListener(playerListener)
            // setSessionAvailabilityListener(sessionAvailabilityListener) // This is one way
            // The existing sessionManagerListener also handles switching logic.
        }
        // If a cast session is already available, make castPlayer active
        // This is also handled by the logic in onCreate: activePlayer = if (currentSession != null) castPlayer else localPlayer
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingValue: Boolean) {
            // Update notification based on activePlayer.isPlaying
            val station = _currentPlayingStation.value // Use instance LiveData
            val statusKey = if (activePlayer is CastPlayer) {
                if (isPlayingValue) "notification_status_casting" else "notification_status_casting_paused"
            } else {
                if (isPlayingValue) "notification_status_playing" else "notification_status_paused"
            }
            if (isPlayingValue) {
                startForeground(NOTIFICATION_ID, createNotification(station, statusKey, sleepTimerEndTimeMillis))
            } else {
                if (activePlayer?.playbackState != Player.STATE_IDLE && activePlayer?.playbackState != Player.STATE_ENDED) {
                    startForeground(NOTIFICATION_ID, createNotification(station, statusKey, sleepTimerEndTimeMillis))
                } else {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
            _isPlaying.postValue(isPlayingValue)

            // Fallback logic for _currentPlayingStation (not static currentPlayingStationLiveData)
            if (isPlayingValue && _currentPlayingStation.value == null) { // Check instance LiveData
                activePlayer?.currentMediaItem?.let { mediaItem ->
                    val stationFromTag = mediaItem.localConfiguration?.tag as? RadioStation
                    if (stationFromTag != null) {
                        _currentPlayingStation.postValue(stationFromTag)
                        // Log for instance update
                        android.util.Log.d("StreamingService", "Fallback: Updated _currentPlayingStation from MediaItem.tag.")
                    } else {
                        serviceScope.launch {
                            try {
                                val stationFromRepo = stationRepository.getStationById(mediaItem.mediaId).firstOrNull()
                                if (stationFromRepo != null) {
                                    _currentPlayingStation.postValue(stationFromRepo)
                                    android.util.Log.d("StreamingService", "Fallback: Updated _currentPlayingStation from Repository using MediaItem.mediaId.")
                                } else {
                                    android.util.Log.w("StreamingService", "Fallback: Repository lookup for Media ID ${mediaItem.mediaId} returned null.")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("StreamingService", "Fallback: Error accessing repository for station. Media ID: ${mediaItem.mediaId}", e)
                            }
                        }
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val playerName = if (activePlayer == localPlayer) "localPlayer" else if (activePlayer == castPlayer) "castPlayer" else "unknownPlayer"
            android.util.Log.e("StreamingService", "ExoPlayer Error from $playerName: ${error.errorCodeName} - ${error.localizedMessage}", error) // Enhanced log

            // activePlayer?.stop() // Decide if stopping is always the right action on error
            // activePlayer?.clearMediaItems() // Decide if clearing items is appropriate

            _isPlaying.postValue(false) // Update INSTANCE LiveData

            // User-friendly message construction
            val userFriendlyMessage = "Error playing stream: ${error.localizedMessage ?: "Unknown playback error"}"
            _playerError.postValue(userFriendlyMessage) // Update INSTANCE LiveData for error

            // Temporarily comment out to keep mini player visible on error for debugging
            // _currentPlayingStation.postValue(null)

            // Toast removed, will be handled by observers
            // stopForeground(STOP_FOREGROUND_DETACH) // Decide if foreground should stop on all errors
        }

        // We can also listen to onMediaItemTransition or onPlaybackStateChanged for history logging
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && activePlayer?.playWhenReady == true) {
                // This is a good place to log to history if we have the station object
                // For simplicity, keeping history logging in onStartCommand for now
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        // val streamUrl = intent?.getStringExtra(EXTRA_STREAM_URL) // No longer needed from intent directly
        val station = intent?.getParcelableExtra<RadioStation>(EXTRA_STATION_OBJECT)

        when (action) {
            ACTION_PLAY -> {
                if (station != null && station.streamUrl.isNotBlank()) { // Changed condition
                    if (requestAudioFocus()) {
                        val mediaItemToPlay: MediaItem
                        if (activePlayer == castPlayer) {
                            android.util.Log.d("StreamingService", "Building MediaItem for CastPlayer (ACTION_PLAY). URI: ${station.streamUrl}") // New Log
                            // Simplify MediaItem for Cast if activePlayer is CastPlayer
                            val appName = getString(R.string.app_name) // Ensure appName is available or re-fetch
                            val castMediaMetadata = MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setArtist(appName)
                                .setAlbumTitle(station.genre ?: "")
                                // .setArtworkUri(Uri.parse(station.favicon)) // Artwork for cast might need specific handling
                                .build()
                            mediaItemToPlay = MediaItem.Builder()
                                .setUri(station.streamUrl) // Use station.streamUrl
                                .setMediaId(station.id) // Keep station.id as it's simple
                                // Avoid .setTag(station) for CastPlayer unless using a custom MediaItemConverter
                                .setMediaMetadata(castMediaMetadata) // Add this line
                                .build()
                        } else {
                            android.util.Log.d("StreamingService", "Building MediaItem for localPlayer (ACTION_PLAY). URI: ${station.streamUrl}") // New Log
                            val appName = getString(R.string.app_name)
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setArtist(appName)
                                .setAlbumTitle(station.genre ?: "") // Use genre for album title
                                // You could add artworkUri here if you have a URI for the station icon/favicon
                                // .setArtworkUri(Uri.parse(station.favicon))
                                .build()
                            mediaItemToPlay = MediaItem.Builder()
                                .setUri(station.streamUrl) // Use station.streamUrl
                                .setMediaId(station.id)
                                .setTag(station) // Keep for local player
                                .setMediaMetadata(mediaMetadata) // Add this line
                                .build()
                        }

                        // Ensure LiveData is updated before prepare() and play()
                        _currentPlayingStation.postValue(station) // Update current station

                        android.util.Log.d("StreamingService", "Setting MediaItem on activePlayer: ${mediaItemToPlay.mediaId}") // New Log
                        activePlayer?.setMediaItem(mediaItemToPlay)
                        activePlayer?.prepare()
                        android.util.Log.d("StreamingService", "Calling play on activePlayer.") // New Log
                        activePlayer?.play() // This should trigger onIsPlayingChanged(true) via listener
                        _isPlaying.postValue(true) // Set explicitly after play()


                        // Log to history using Repository and serviceScope
                        serviceScope.launch {
                            stationRepository.addStationToHistory(station)
                        }
                    }
                } else if (activePlayer?.isPlaying == false && activePlayer?.mediaItemCount ?: 0 > 0) {
                    // Resume case
                    if (requestAudioFocus()) {
                        android.util.Log.d("StreamingService", "Resuming play on activePlayer.") // New Log
                        activePlayer?.play() // This should trigger onIsPlayingChanged(true)
                        _isPlaying.postValue(true) // Set explicitly after play()
                    }
                }
            }
            ACTION_PAUSE -> {
                activePlayer?.pause() // This should trigger onIsPlayingChanged(false)
            }
            ACTION_STOP -> {
                activePlayer?.stop()
                activePlayer?.clearMediaItems()
                _isPlaying.postValue(false)
                _currentPlayingStation.postValue(null)
                abandonAudioFocus()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SET_SLEEP_TIMER -> {
                val durationMillis = intent?.getLongExtra(EXTRA_SLEEP_DURATION_MS, 0) ?: 0
                setSleepTimer(durationMillis)
            }
            ACTION_CANCEL_SLEEP_TIMER -> {
                cancelSleepTimer()
            }
        }
        return START_STICKY
    }

    private fun setSleepTimer(durationMillis: Long) {
        cancelSleepTimer() // Cancel any existing timer
        if (durationMillis > 0) {
            sleepTimerEndTimeMillis = System.currentTimeMillis() + durationMillis
            sleepTimerRunnable = Runnable {
                activePlayer?.pause()
                Toast.makeText(applicationContext, getString(R.string.toast_sleep_timer_ended), Toast.LENGTH_SHORT).show()
                sleepTimerEndTimeMillis = 0L // Reset
                updateNotificationState() // Update notification
            }
            handler.postDelayed(sleepTimerRunnable!!, durationMillis)
            Toast.makeText(applicationContext, getString(R.string.toast_sleep_timer_set, durationMillis / 60000), Toast.LENGTH_SHORT).show()
            updateNotificationState() // Update notification
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let {
            handler.removeCallbacks(it)
            sleepTimerRunnable = null
            Toast.makeText(applicationContext, getString(R.string.toast_sleep_timer_cancelled), Toast.LENGTH_SHORT).show()
        }
        sleepTimerEndTimeMillis = 0L
        updateNotificationState() // Update notification
    }

    private fun updateNotificationState() {
        // Only update notification if service is in a state where it should be showing one
        if (activePlayer?.playbackState != Player.STATE_IDLE && activePlayer?.playbackState != Player.STATE_ENDED || _isPlaying.value == true) {
            val station = _currentPlayingStation.value
            val isPlayingVal = _isPlaying.value ?: false // Default to false if null
            val statusKey = if (activePlayer is CastPlayer) {
                if (isPlayingVal) "notification_status_casting" else "notification_status_casting_paused"
            } else {
                if (isPlayingVal) "notification_status_playing" else "notification_status_paused"
            }
            startForeground(NOTIFICATION_ID, createNotification(station, statusKey, sleepTimerEndTimeMillis))
        } else {
             // If not playing and in idle/ended state, consider stopping foreground or showing a different notification
             // For now, let's assume stopForeground(STOP_FOREGROUND_DETACH) is handled by onIsPlayingChanged
        }
    }

    private fun switchToCastPlayer() {
        android.util.Log.d("StreamingService", "Attempting to switch to CastPlayer.") // New Log
        if (activePlayer == castPlayer && castPlayer?.isCastSessionAvailable == true) {
            android.util.Log.d("StreamingService", "Already on CastPlayer and session available. No switch needed.") // New Log
            return
        }

        val currentLocalMediaItem = activePlayer?.currentMediaItem // Could be from localPlayer
        val currentPosition = activePlayer?.currentPosition ?: 0
        val playWhenReady = activePlayer?.playWhenReady ?: false

        android.util.Log.d("StreamingService", "Stopping localPlayer for cast switch.") // New Log
        localPlayer?.stop()
        activePlayer = castPlayer
        android.util.Log.d("StreamingService", "Active player is now castPlayer.") // New Log

        currentLocalMediaItem?.let { localItem ->
            val castUri = localItem.requestMetadata.mediaUri
            android.util.Log.d("StreamingService", "Building MediaItem for CastPlayer from localItem. URI: $castUri") // New Log
            if (castUri == null) {
                android.util.Log.e("StreamingService", "Cannot switch to cast: localItem mediaUri is null.")
                return@let
            }
            // Simplify MediaItem for Cast: only URI, and potentially mediaId if simple.
            // Avoid complex tags unless a custom MediaItemConverter is in place.
            val currentStation = _currentPlayingStation.value // Get current station from INSTANCE LiveData
            val appName = getString(R.string.app_name)
            var castMediaMetadataBuilder = MediaMetadata.Builder()
                .setArtist(appName) // App name is always known

            currentStation?.let {
                castMediaMetadataBuilder = castMediaMetadataBuilder
                    .setTitle(it.name)
                    .setAlbumTitle(it.genre ?: "")
                    // .setArtworkUri(Uri.parse(it.favicon))
            }
            // If currentStation is null, title and album might be missing, but artist (app name) will be set.
            // Alternatively, extract title/album from localItem.mediaMetadata if it was set,
            // but we are setting it fresh based on station data.

            val castMediaItem = MediaItem.Builder()
                .setUri(castUri)
                .setMediaId(localItem.mediaId ?: castUri.toString()) // Use local mediaId or URI as backup
                .setMediaMetadata(castMediaMetadataBuilder.build()) // Add this line
                .build()

            android.util.Log.d("StreamingService", "Setting MediaItem on CastPlayer: ${castMediaItem.mediaId}") // New Log
            castPlayer?.setMediaItem(castMediaItem, currentPosition)
            castPlayer?.playWhenReady = playWhenReady
            castPlayer?.prepare()
            if (playWhenReady) {
                android.util.Log.d("StreamingService", "Calling play on CastPlayer.") // New Log
                castPlayer?.play()
            }
        } ?: run {
            android.util.Log.d("StreamingService", "switchToCastPlayer: No current MediaItem to transfer.") // New Log
        }
        startForeground(NOTIFICATION_ID, createNotification(_currentPlayingStation.value, "notification_status_casting", sleepTimerEndTimeMillis))
    }

    private fun switchToLocalPlayer() {
        if (activePlayer == localPlayer) return

        val currentMediaItem = activePlayer?.currentMediaItem
        val currentPosition = activePlayer?.currentPosition ?: 0
        val playWhenReady = activePlayer?.playWhenReady ?: false

        castPlayer?.stop() // Stop cast playback
        activePlayer = localPlayer

        currentMediaItem?.let {
            localPlayer?.setMediaItem(it, currentPosition)
            localPlayer?.playWhenReady = playWhenReady
            localPlayer?.prepare()
            if(playWhenReady) localPlayer?.play()
        }
         // Update notification, UI, etc.
        if (localPlayer?.isPlaying == true) {
            startForeground(NOTIFICATION_ID, createNotification(_currentPlayingStation.value, "notification_status_playing", sleepTimerEndTimeMillis))
        } else {
            stopForeground(STOP_FOREGROUND_DETACH) // Or update to paused
        }
    }


    private fun requestAudioFocus(): Boolean {
        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .build()
            result = audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (activePlayer?.isPlaying == true) { // Check if it was actually playing
                    pausedByAudioFocusLoss = true
                }
                pausePlayback() // Or stop, depending on desired behavior
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (activePlayer?.isPlaying == true) { // Check if it was actually playing
                    pausedByAudioFocusLoss = true
                }
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                activePlayer?.volume = 0.3f // Lower volume
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                activePlayer?.volume = 1.0f // Restore volume
                if (pausedByAudioFocusLoss && activePlayer?.playWhenReady == false && activePlayer?.playbackState == Player.STATE_READY) {
                    activePlayer?.play()
                }
                pausedByAudioFocusLoss = false // Reset the flag
            }
        }
    }

    private fun pausePlayback() {
        // JULES_VERIFICATION_COMMENT_PAUSEPLAYBACK_FIX_ATTEMPT_2
        activePlayer?.playWhenReady = false
    }

    // These methods are now effectively handled by activePlayer.play(), activePlayer.pause(), etc.
    // Keeping them for conceptual clarity or if direct calls are needed.
    // fun setMediaSource(url: String) { ... }
    // fun startPlayback() { activePlayer?.playWhenReady = true }
    // fun pausePlayback() { activePlayer?.playWhenReady = false }
    // fun stopPlayback() { activePlayer?.stop(); activePlayer?.clearMediaItems() }

    // fun clearPlayerError() {
    //     _playerError.postValue(null)
    // }


    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
        serviceScope.cancel() // Cancel all coroutines started by this service
        localPlayer?.release()
        localPlayer = null
        castPlayer?.release() // Release CastPlayer, now nullable
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        // castPlayer?.setSessionAvailabilityListener(null) // if using this listener
        abandonAudioFocus()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder // Or null if you don't support binding from this entry point
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Streaming Service Channel",
                NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound for ongoing notification
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(station: RadioStation?, contentTextHintKey: String, sleepTimerEndTimeMillis: Long = 0L): Notification {
        val title = station?.name ?: getString(R.string.notification_title_default) // Use string resource
        var text = getString(resources.getIdentifier(contentTextHintKey, "string", packageName)) // Get hint from resource

        if (sleepTimerEndTimeMillis > 0) {
            val remainingMinutes = (sleepTimerEndTimeMillis - System.currentTimeMillis()) / 60000
            if (remainingMinutes > 0) {
                text += " (${getString(R.string.notification_sleep_timer_active, remainingMinutes)})"
            }
        }
        // ... rest of the notification build (intents, actions) remains same
        // Make sure to use string resources for "Play" and "Pause" actions too.
        val playActionText = getString(R.string.notification_action_play)
        val pauseActionText = getString(R.string.notification_action_pause)

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val playIntent = Intent(this, StreamingService::class.java).apply { action = ACTION_PLAY }
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, pendingIntentFlags)

        val pauseIntent = Intent(this, StreamingService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_radio_placeholder)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_play_arrow, playActionText, playPendingIntent)
            .addAction(R.drawable.ic_pause, pauseActionText, pausePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
