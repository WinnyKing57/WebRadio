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

    fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.isEnabled) {
            Log.d("AlarmManagerHelper", "Alarm ${alarm.id} is disabled, not scheduling.")
            return
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
                Log.w("AlarmManagerHelper", "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission or user setting enabled.")
                // Optionally, notify the user or fallback to inexact alarm.
                // For now, we'll still try to set it, but it might behave as inexact.
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("AlarmManagerHelper", "Alarm ${alarm.id} scheduled for ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("AlarmManagerHelper", "SecurityException: Cannot schedule exact alarms. Check permissions.", e)
            // Notify user or handle appropriately
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
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm) // Ensure disabled alarms are not scheduled
            }
        }
    }
}
