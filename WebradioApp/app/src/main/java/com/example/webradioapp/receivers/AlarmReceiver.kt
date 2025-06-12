package com.example.webradioapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.services.StreamingService
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.utils.AlarmManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "com.example.webradioapp.EXTRA_ALARM_ID"
        const val EXTRA_STATION_ID = "com.example.webradioapp.EXTRA_STATION_ID"
        const val EXTRA_STATION_NAME = "com.example.webradioapp.EXTRA_STATION_NAME"
        const val EXTRA_STATION_ICON_URL = "com.example.webradioapp.EXTRA_STATION_ICON_URL"
        // It's better to fetch the full station details (including streamUrl) from DB
        // using stationId to ensure data is up-to-date.
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val stationId = intent.getStringExtra(EXTRA_STATION_ID)
        // val stationName = intent.getStringExtra(EXTRA_STATION_NAME) // Can be used for notification
        // val stationIconUrl = intent.getStringExtra(EXTRA_STATION_ICON_URL) // For notification

        if (alarmId == -1 || stationId == null) {
            Log.e("AlarmReceiver", "Alarm ID or Station ID missing in intent.")
            return
        }

        // Fetch full station details from database to get the stream URL
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context.applicationContext)
            // Assumption: RadioStation table might contain the station if it was favorited or played.
            // A more robust solution would be to ensure the station details for alarms are always available,
            // perhaps by querying a specific "all stations" table or API.
            // For now, let's try to find it in favorites or history.
            var station = database.favoriteStationDao().getFavoriteStationById(stationId)
            if (station == null) {
                station = database.historyStationDao().getHistoryStationById(stationId)
            }
            // If still null, it means the station is not in local DB tables accessible this way.
            // This highlights the need for a reliable way to get station stream URLs for alarms.
            // For this example, we'll proceed if found, log error if not.

            if (station == null || station.streamUrl.isNullOrEmpty()) {
                Log.e("AlarmReceiver", "Station details or stream URL for ID $stationId not found. Cannot start playback.")
                // Optionally, show a notification to the user about the failure.
                return@launch
            }

            val serviceIntent = Intent(context, StreamingService::class.java).apply {
                action = StreamingService.ACTION_PLAY
                putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                // The stream URL is part of the RadioStation object now
            }
            context.startService(serviceIntent)
            Log.d("AlarmReceiver", "Started StreamingService for station: ${station.name}")

            // Reschedule for next day (for repeating alarms) or mark as complete.
            // For non-repeating alarms, we might disable it.
            // For this example, let's assume alarms are one-shot and need to be re-enabled manually
            // or the app provides explicit repeat settings.
            // If it's a one-time alarm, we might want to disable it in the database.
            val alarm = database.alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                // Example: If you want one-shot alarms to disable themselves:
                // database.alarmDao().update(alarm.copy(isEnabled = false))
                // Log.d("AlarmReceiver", "Alarm ${alarm.id} marked as disabled after firing.")

                // If alarms are meant to repeat daily by default with this setup:
                val alarmManagerHelper = AlarmManagerHelper(context)
                alarmManagerHelper.scheduleAlarm(alarm) // This will schedule it for the next day.
                Log.d("AlarmReceiver", "Alarm ${alarm.id} rescheduled for the next day.")
            }
        }
    }
}
