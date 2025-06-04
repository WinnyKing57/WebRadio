package com.example.webradioapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.webradioapp.AlarmReceiver
import com.example.webradioapp.model.RadioStation
import com.google.gson.Gson

object AlarmScheduler {

    private const val ALARM_REQUEST_CODE = 12345
    private const val PREF_KEY_ALARM_TIME = "alarm_time_millis"
    private const val PREF_KEY_ALARM_STATION_JSON = "alarm_station_json"


    fun scheduleAlarm(context: Context, timeInMillis: Long, stationToPlay: RadioStation?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
            if (stationToPlay != null) {
                putExtra(AlarmReceiver.EXTRA_STATION_JSON, Gson().toJson(stationToPlay))
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        // Using setAndAllowWhileIdle for broader compatibility first.
        // For precise alarms, setExactAndAllowWhileIdle would be used,
        // but it requires SCHEDULE_EXACT_ALARM permission handling on Android 12+.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
            Log.d("AlarmScheduler", "Alarm scheduled for $timeInMillis")
            // Save alarm details
            saveAlarmDetails(context, timeInMillis, stationToPlay)
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException while scheduling alarm. Check for SCHEDULE_EXACT_ALARM if using setExact.", e)
            // Inform user that permission might be needed
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGERED
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Alarm cancelled.")
        }
        clearAlarmDetails(context)
    }

    private fun saveAlarmDetails(context: Context, timeInMillis: Long, station: RadioStation?) {
        val prefs = SharedPreferencesManager(context) // Assuming SharedPreferencesManager can handle generic key-value
        // This is not ideal as SharedPreferencesManager is specific.
        // For simplicity, using its underlying prefs object if possible, or add generic methods to it.
        // Or, create a new shared preference file just for alarm.
        val editor = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit()
        editor.putLong(PREF_KEY_ALARM_TIME, timeInMillis)
        if (station != null) {
            editor.putString(PREF_KEY_ALARM_STATION_JSON, Gson().toJson(station))
        } else {
            editor.remove(PREF_KEY_ALARM_STATION_JSON)
        }
        editor.apply()
    }

    private fun clearAlarmDetails(context: Context) {
        val editor = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit()
        editor.remove(PREF_KEY_ALARM_TIME)
        editor.remove(PREF_KEY_ALARM_STATION_JSON)
        editor.apply()
    }

    fun getScheduledAlarmTime(context: Context): Long {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        return prefs.getLong(PREF_KEY_ALARM_TIME, 0L)
    }

    fun getScheduledAlarmStation(context: Context): RadioStation? {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_KEY_ALARM_STATION_JSON, null)
        return json?.let { Gson().fromJson(it, RadioStation::class.java) }
    }
}
