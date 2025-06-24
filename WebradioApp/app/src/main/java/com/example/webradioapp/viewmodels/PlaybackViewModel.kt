package com.example.webradioapp.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.services.StreamingService

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPlayingStation = MutableLiveData<RadioStation?>()
    val currentPlayingStation: LiveData<RadioStation?> = _currentPlayingStation

    private val _playerError = MutableLiveData<String?>() // For single error events
    val playerError: LiveData<String?> = _playerError

    private var streamingService: StreamingService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StreamingService.LocalBinder
            streamingService = binder.getService()
            isBound = true
            Log.d("PlaybackViewModel", "StreamingService connected")

            // Start observing LiveData from the service instance
            streamingService?.isPlaying?.observeForever(isPlayingObserver)
            streamingService?.currentPlayingStation?.observeForever(currentStationObserver)
            streamingService?.playerError?.observeForever(playerErrorObserver)

            // Explicitly update ViewModel's LiveData with current values from the service.
            // This is a defensive measure; observeForever should ideally handle this by
            // immediately invoking the observer with the current value if one exists.
            currentStationObserver.invoke(streamingService?.currentPlayingStation?.value)
            isPlayingObserver.invoke(streamingService?.isPlaying?.value ?: false)
            playerErrorObserver.invoke(streamingService?.playerError?.value)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("PlaybackViewModel", "StreamingService disconnected")
            // Stop observing
            streamingService?.isPlaying?.removeObserver(isPlayingObserver)
            streamingService?.currentPlayingStation?.removeObserver(currentStationObserver)
            streamingService?.playerError?.removeObserver(playerErrorObserver)

            streamingService = null
            isBound = false
        }
    }

    private val isPlayingObserver: (Boolean) -> Unit = { _isPlaying.postValue(it) }
    private val currentStationObserver: (RadioStation?) -> Unit = { _currentPlayingStation.postValue(it) }
    private val playerErrorObserver: (String?) -> Unit = {
        _playerError.postValue(it)
        // Optional: Clear error after a short delay or when consumed if it's a one-time event
        if (it != null) {
            // streamingService?.clearPlayerError() // Call a method on service to clear its error state
        }
    }

    init {
        bindToStreamingService()
    }

    private fun bindToStreamingService() {
        Intent(getApplication(), StreamingService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun playStation(station: RadioStation) {
        if (isBound) {
            val intent = Intent(getApplication(), StreamingService::class.java).apply {
                action = StreamingService.ACTION_PLAY
                putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
            }
            getApplication<Application>().startService(intent)
        } else {
            Log.w("PlaybackViewModel", "Service not bound, cannot play station")
            // Optionally, queue the action or try to rebind
        }
    }

    fun pausePlayback() {
        if (isBound) {
            val intent = Intent(getApplication(), StreamingService::class.java).apply {
                action = StreamingService.ACTION_PAUSE
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun stopPlayback() {
         if (isBound) {
             val intent = Intent(getApplication(), StreamingService::class.java).apply {
                 action = StreamingService.ACTION_STOP
             }
             getApplication<Application>().startService(intent)
         }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            // Stop observing before unbinding
            streamingService?.isPlaying?.removeObserver(isPlayingObserver)
            streamingService?.currentPlayingStation?.removeObserver(currentStationObserver)
            streamingService?.playerError?.removeObserver(playerErrorObserver)

            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
            streamingService = null
        }
    }
}
