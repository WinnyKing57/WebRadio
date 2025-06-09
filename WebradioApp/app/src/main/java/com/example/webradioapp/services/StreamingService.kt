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
import android.widget.Toast // Added import
import androidx.core.app.NotificationCompat
import com.example.webradioapp.R
import com.example.webradioapp.activities.MainActivity
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.utils.SharedPreferencesManager
import androidx.media3.exoplayer.ExoPlayer // Changed
import androidx.media3.common.MediaItem // Changed
import androidx.media3.common.PlaybackException // Changed
import androidx.media3.common.Player // Changed
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

/**
 * Foreground service for handling audio streaming and Cast playback.
 * Manages ExoPlayer for local playback and CastPlayer for Chromecast sessions.
 * Handles audio focus, media notifications, sleep timer, and history logging.
 */
class StreamingService : Service(), AudioManager.OnAudioFocusChangeListener {

    private var localPlayer: ExoPlayer? = null // Renamed from player to localPlayer
    private lateinit var castPlayer: CastPlayer
    private lateinit var castContext: CastContext
    private lateinit var sessionManager: SessionManager
    private var currentSession: CastSession? = null

    private val binder = LocalBinder()
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var sharedPrefsManager: SharedPreferencesManager // Still used for Theme
    private lateinit var stationRepository: com.example.webradioapp.db.StationRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


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
        sharedPrefsManager = SharedPreferencesManager(applicationContext) // For theme

        val database = com.example.webradioapp.db.AppDatabase.getDatabase(applicationContext)
        stationRepository = com.example.webradioapp.db.StationRepository(database.favoriteStationDao(), database.historyStationDao())

        castContext = CastContext.getSharedInstance(this)
        sessionManager = castContext.sessionManager
        currentSession = sessionManager.currentCastSession

        initializeLocalPlayer()
        initializeCastPlayer()

        activePlayer = if (currentSession != null) castPlayer else localPlayer
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        // castPlayer.setSessionAvailabilityListener(sessionAvailabilityListener) // Alternative listener

        createNotificationChannel()
    }

    private fun initializeLocalPlayer() {
        if (localPlayer == null) {
            localPlayer = ExoPlayer.Builder(this).build().apply {
                addListener(playerListener)
            }
        }
    }

    private fun initializeCastPlayer() {
        castPlayer = CastPlayer(castContext).apply {
            addListener(playerListener)
            // setSessionAvailabilityListener(sessionAvailabilityListener) // if using this listener
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Update notification based on activePlayer.isPlaying
            if (activePlayer?.isPlaying == true) {
                startForeground(NOTIFICATION_ID, createNotification(if (activePlayer is CastPlayer) "Casting" else "Playing"))
            } else {
                 // Update notification to paused state or stop foreground if not playing
                if (activePlayer?.playbackState != Player.STATE_IDLE && activePlayer?.playbackState != Player.STATE_ENDED) {
                     startForeground(NOTIFICATION_ID, createNotification(if (activePlayer is CastPlayer) "Casting Paused" else "Paused"))
                } else {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopSelf()
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
        val streamUrl = intent?.getStringExtra(EXTRA_STREAM_URL)
        val station = intent?.getParcelableExtra<RadioStation>(EXTRA_STATION_OBJECT)

        when (action) {
            ACTION_PLAY -> {
                if (streamUrl != null && station != null) {
                    if (requestAudioFocus()) {
                        val mediaItem = MediaItem.Builder()
                            .setUri(streamUrl)
                            .setMediaId(station.id)
                            .setTag(station) // Keep station object for later
                            .build()

                        activePlayer?.setMediaItem(mediaItem)
                        activePlayer?.prepare()
                        activePlayer?.play()

                        // Log to history using Repository and serviceScope
                        serviceScope.launch {
                            stationRepository.addStationToHistory(station)
                        }

                        // Notification will be updated by playerListener.onIsPlayingChanged
                    }
                } else if (activePlayer?.isPlaying == false && activePlayer?.mediaItemCount ?: 0 > 0) {
                    // Resume case
                    if (requestAudioFocus()) {
                        activePlayer?.play()
                    }
                }
            }
            ACTION_PAUSE -> {
                activePlayer?.pause()
                // Notification updated by listener
            }
            ACTION_STOP -> {
                activePlayer?.stop()
                activePlayer?.clearMediaItems()
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
                // Pause playback, could also be stopSelf() depending on desired behavior
                activePlayer?.pause()
                // Optionally, update notification or inform UI
                Toast.makeText(applicationContext, "Sleep timer ended.", Toast.LENGTH_SHORT).show()
                sleepTimerEndTimeMillis = 0L // Reset
            }
            handler.postDelayed(sleepTimerRunnable!!, durationMillis)
            Toast.makeText(applicationContext, "Sleep timer set for ${durationMillis / 60000} minutes.", Toast.LENGTH_SHORT).show()
            // TODO: Update notification to show timer is active
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let {
            handler.removeCallbacks(it)
            sleepTimerRunnable = null
            Toast.makeText(applicationContext, "Sleep timer cancelled.", Toast.LENGTH_SHORT).show()
        }
        sleepTimerEndTimeMillis = 0L
        // TODO: Update notification to remove timer indication
    }

    private fun switchToCastPlayer() {
        if (activePlayer == castPlayer && castPlayer.isCastSessionAvailable) return

        val currentMediaItem = activePlayer?.currentMediaItem
        val currentPosition = activePlayer?.currentPosition ?: 0
        val playWhenReady = activePlayer?.playWhenReady ?: false

        localPlayer?.stop() // Stop local playback
        activePlayer = castPlayer

        currentMediaItem?.let {
            castPlayer.setMediaItem(it, currentPosition)
            castPlayer.playWhenReady = playWhenReady
            castPlayer.prepare()
            if(playWhenReady) castPlayer.play()
        }
        // Update notification, UI, etc.
        startForeground(NOTIFICATION_ID, createNotification("Casting"))
    }

    private fun switchToLocalPlayer() {
        if (activePlayer == localPlayer) return

        val currentMediaItem = activePlayer?.currentMediaItem
        val currentPosition = activePlayer?.currentPosition ?: 0
        val playWhenReady = activePlayer?.playWhenReady ?: false

        castPlayer.stop() // Stop cast playback
        activePlayer = localPlayer

        currentMediaItem?.let {
            localPlayer?.setMediaItem(it, currentPosition)
            localPlayer?.playWhenReady = playWhenReady
            localPlayer?.prepare()
            if(playWhenReady) localPlayer?.play()
        }
         // Update notification, UI, etc.
        if (localPlayer?.isPlaying == true) {
            startForeground(NOTIFICATION_ID, createNotification("Playing"))
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
                pausePlayback() // Or stop, depending on desired behavior
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                activePlayer?.volume = 0.3f // Lower volume
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                activePlayer?.volume = 1.0f // Restore volume
                 if (activePlayer?.playWhenReady == false && activePlayer?.playbackState == Player.STATE_READY) {
                    // Resume if paused due to transient loss, and if it's appropriate to do so
                    // activePlayer?.play()
                }
            }
        }
    }

    private fun pausePlayback() {
        activePlayer?.playWhenReady = false
        // Log.d("StreamingService", "Playback paused via pausePlayback()") // Optional logging
    }

    // These methods are now effectively handled by activePlayer.play(), activePlayer.pause(), etc.
    // Keeping them for conceptual clarity or if direct calls are needed.
    // fun setMediaSource(url: String) { ... }
    // fun startPlayback() { activePlayer?.playWhenReady = true }
    // fun pausePlayback() { activePlayer?.playWhenReady = false }
    // fun stopPlayback() { activePlayer?.stop(); activePlayer?.clearMediaItems() }


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel all coroutines started by this service
        localPlayer?.release()
        localPlayer = null
        castPlayer.release() // Release CastPlayer
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        // castPlayer.setSessionAvailabilityListener(null) // if using this listener
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

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        // Add actions for Play/Pause to the notification
        val playIntent = Intent(this, StreamingService::class.java).apply { action = ACTION_PLAY }
        val playPendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, playPendingIntentFlags)

        val pauseIntent = Intent(this, StreamingService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, pausePendingIntentFlags)


        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Webradio Playing")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_radio_placeholder) // Replace with actual icon
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_play_arrow, "Play", playPendingIntent) // Replace with actual icon
            .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent) // Replace with actual icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Makes the notification non-dismissable while service is in foreground
            .build()
    }
}
