package com.example.webradioapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.webradioapp.model.Alarm
import com.example.webradioapp.receivers.AlarmReceiver
import java.util.Calendar

class AlarmManagerHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm): Boolean { // Added return type
        if (!alarm.isEnabled) {
            Log.d("AlarmManagerHelper", "Alarm ${alarm.id} is disabled, not scheduling.")
            return true // Or false, depending on how you define success for disabled alarms
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_STATION_ID, alarm.stationId)
            putExtra(AlarmReceiver.EXTRA_STATION_NAME, alarm.stationName)
            putExtra(AlarmReceiver.EXTRA_STATION_ICON_URL, alarm.stationIconUrl)
            // Add other necessary details like stream URL if not fetched by ID in receiver
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // Use alarm.id as the requestCode to ensure uniqueness for each alarm's PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(context, alarm.id, intent, pendingIntentFlags)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the alarm time is in the past for today, schedule it for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmManagerHelper", "Cannot schedule exact alarm ${alarm.id}. The SCHEDULE_EXACT_ALARM permission is denied by the user or the capability is unavailable. Alarm will not be set.")
                // TODO: Notify user through UI (e.g., via ViewModel callback or event)
                // Consider if this should throw an exception or how to propagate failure clearly.
                return false // Indicate failure
            }

            // If the check passes (or API < S), proceed to set the alarm.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("AlarmManagerHelper", "Alarm ${alarm.id} scheduled for ${calendar.time}")
            return true // Indicate success
        } catch (e: SecurityException) {
            Log.e("AlarmManagerHelper", "SecurityException: Cannot schedule exact alarm ${alarm.id}. Check permissions (USE_EXACT_ALARM might be required and not granted on Android 14+ if app targets API 33+).", e)
            // TODO: Notify user through UI
            return false // Indicate failure
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        cancelAlarmById(alarm.id)
    }

    fun cancelAlarmById(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java) // Intent must match the one used for scheduling
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // NO_CREATE to check existence
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Also cancel the PendingIntent itself
            Log.d("AlarmManagerHelper", "Alarm $alarmId cancelled.")
        } else {
            Log.d("AlarmManagerHelper", "Alarm $alarmId not found or already cancelled.")
        }
    }

    fun rescheduleAllAlarms(alarms: List<Alarm>) {
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                val scheduled = scheduleAlarm(alarm) // scheduleAlarm now returns boolean
                if (!scheduled) {
                    Log.w("AlarmManagerHelper", "Failed to reschedule alarm ${alarm.id}")
                    // Optionally, update alarm's state in DB to reflect it's not properly scheduled
                }
            } else {
                cancelAlarm(alarm)
            }
        }
    }
}
