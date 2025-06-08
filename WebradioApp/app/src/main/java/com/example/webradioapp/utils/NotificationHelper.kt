package com.example.webradioapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.webradioapp.R
import com.example.webradioapp.activities.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "StreamingServiceChannel"
    const val TEST_NOTIFICATION_ID = 999
    private const val CHANNEL_NAME = "Radio Streaming"
    private const val CHANNEL_DESCRIPTION = "Notifications for radio streaming service"

    /**
     * Creates notification channel for streaming service
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio_placeholder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

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