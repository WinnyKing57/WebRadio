package com.example.webradioapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * BroadcastReceiver for handling alarm notifications
 * Used for sleep timer functionality
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SLEEP_TIMER = "com.example.webradioapp.SLEEP_TIMER"
        const val EXTRA_MESSAGE = "message"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            ACTION_SLEEP_TIMER -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Sleep timer ended"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                // Stop any ongoing streaming service
                val serviceIntent = Intent(context, com.example.webradioapp.services.StreamingService::class.java).apply {
                    action = com.example.webradioapp.services.StreamingService.ACTION_STOP
                }
                context.startService(serviceIntent)
            }
        }
    }
}