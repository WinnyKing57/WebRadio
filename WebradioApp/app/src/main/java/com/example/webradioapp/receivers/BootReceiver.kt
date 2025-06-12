package com.example.webradioapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.utils.AlarmManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device boot completed. Rescheduling alarms.")
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getDatabase(context.applicationContext)
                val alarms = database.alarmDao().getEnabledAlarms() // Fetch only enabled alarms
                if (alarms.isNotEmpty()) {
                    val alarmManagerHelper = AlarmManagerHelper(context)
                    alarmManagerHelper.rescheduleAllAlarms(alarms)
                    Log.d("BootReceiver", "${alarms.size} alarms rescheduled.")
                } else {
                    Log.d("BootReceiver", "No enabled alarms to reschedule.")
                }
            }
        }
    }
}
