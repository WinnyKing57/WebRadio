package com.example.webradioapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.webradioapp.R
import com.example.webradioapp.activities.MainActivity

/**
 * Utility object for creating and managing notifications.
 * Handles notification channel creation for Android Oreo (API 26) and above,
 * and provides a helper function to display simple notifications.
 */
object NotificationHelper {

    /**
     * Notification channel ID for general app updates or information.
     * User-facing name for this channel is "Station Updates".
     */
    const val GENERAL_CHANNEL_ID = "station_updates_channel"

    /** A unique ID for the test notification shown from settings. */
    const val TEST_NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel for general app notifications.
     * This should be called once when the application starts.
     * For Android Oreo (API 26) and above, notification channels are required.
     *
     * @param context The application context.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name_station_updates) // Using string resource
            val descriptionText = context.getString(R.string.notification_channel_description_station_updates)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(GENERAL_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a simple notification.
     * Respects the in-app user preference for enabling/disabling this category of notifications.
     * Requires [Manifest.permission.POST_NOTIFICATIONS] on Android 13 (API 33) and above,
     * which should be checked by the caller before invoking this function.
     *
     * @param context The context to use for creating the notification.
     * @param title The title of the notification.
     * @param message The main content/message of the notification.
     * @param notificationId A unique integer ID for this notification.
     */
    fun showSimpleNotification(context: Context, title: String, message: String, notificationId: Int) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio_placeholder) // Ensure this icon is suitable for status bar
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true) // Automatically removes the notification when the user taps it

        // Check app's own notification preference first
        val prefs = SharedPreferencesManager(context)
        if (!prefs.isStationUpdatesNotificationEnabled()) {
            Log.d("NotificationHelper", "Station Updates notifications are disabled by in-app user preference.")
            return // Don't show the notification
        }

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                // This check should ideally be done before calling this function,
                // and permission requested if not granted.
                // For now, if permission is not granted on API 33+, we just won't show it here.
                // The calling fragment will handle the permission request.
                return
            }
            notify(notificationId, builder.build())
        }
    }
}
package com.example.webradioapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.webradioapp.R
import com.example.webradioapp.activities.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "StreamingServiceChannel"
    const val TEST_NOTIFICATION_ID = 999
    private const val CHANNEL_NAME = "Radio Streaming"
    private const val CHANNEL_DESCRIPTION = "Notifications for radio streaming service"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        context: Context,
        title: String,
        content: String,
        isPlaying: Boolean = false,
        stationName: String? = null
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_radio_placeholder)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun showSimpleNotification(context: Context, title: String, content: String, notificationId: Int) {
        val notification = createNotification(context, title, content)
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }
    }
}
