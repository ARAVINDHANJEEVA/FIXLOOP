package com.fixloop.mobile.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fixloop.mobile.MainActivity

object SentinelNotificationHelper {

    const val CHANNEL_ALERTS = "sentinel_alerts_channel"
    const val CHANNEL_URGENT = "sentinel_urgent_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Ticket Updates & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates on status changes, staff dispatch, and ticket reviews"
                enableVibration(true)
            }

            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT,
                "Urgent Action Needed",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate requests for info or critical notices"
                enableVibration(true)
            }

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(urgentChannel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        complaintId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            complaintId?.let { putExtra("EXTRA_COMPLAINT_ID", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted on Android 13+
        }
    }
}
