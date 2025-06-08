package com.example.webradioapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.services.StreamingService
import com.example.webradioapp.utils.SharedPreferencesManager
import com.google.gson.Gson

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGERED = "com.example.webradioapp.ACTION_ALARM_TRIGGERED"
        const val EXTRA_STATION_JSON = "com.example.webradioapp.EXTRA_STATION_JSON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received with action: ${intent.action}")
        if (intent.action == ACTION_ALARM_TRIGGERED) {
            val stationJson = intent.getStringExtra(EXTRA_STATION_JSON)
            val station = stationJson?.let { Gson().fromJson(it, RadioStation::class.java) }
            
            if (station != null) {
                Log.d("AlarmReceiver", "Starting streaming service for station: ${station.name}")
                val serviceIntent = Intent(context, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                }
                context.startForegroundService(serviceIntent)
            } else {
                Log.e("AlarmReceiver", "Station data is null, cannot start streaming")
            }
        }
    }
}

            if (station != null) {
                Log.d("AlarmReceiver", "Starting StreamingService for station: ${station.name}")
                val serviceIntent = Intent(context, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                    // Potentially add a flag if special handling is needed for alarms, e.g., override DND
                }
                // Starting foreground service from broadcast receiver (especially on newer Android versions)
                // might have restrictions. Consider using context.startForegroundService()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                Log.e("AlarmReceiver", "No station data found in alarm intent.")
                // Fallback: Play last played station from history if no specific station
                val sharedPrefsManager = SharedPreferencesManager(context)
                val lastPlayed = sharedPrefsManager.getStationHistory().firstOrNull()
                if (lastPlayed != null) {
                    Log.d("AlarmReceiver", "Fallback: Starting StreamingService for last played: ${lastPlayed.name}")
                    val serviceIntent = Intent(context, StreamingService::class.java).apply {
                        action = StreamingService.ACTION_PLAY
                        putExtra(StreamingService.EXTRA_STREAM_URL, lastPlayed.streamUrl)
                        putExtra(StreamingService.EXTRA_STATION_OBJECT, lastPlayed)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                     Log.e("AlarmReceiver", "No last played station found for fallback.")
                }
            }
        }
    }
}
