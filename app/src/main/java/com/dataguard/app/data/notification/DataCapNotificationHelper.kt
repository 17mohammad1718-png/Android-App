package com.dataguard.app.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dataguard.app.MainActivity
import com.dataguard.app.R
import com.dataguard.app.domain.util.ByteFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages data cap threshold notifications.
 * Requires POST_NOTIFICATIONS permission on Android 13+.
 */
@Singleton
class DataCapNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "data_cap_alerts"
        private const val NOTIFICATION_ID_BASE = 9000
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Send a threshold notification if the usage percent crosses [thresholdPercent].
     * Uses the threshold value as part of the notification ID so that each
     * threshold fires at most once until dismissed.
     */
    fun notifyThresholdIfNeeded(
        percent: Float,
        thresholdPercent: Int,
        usedBytes: Long,
        limitBytes: Long,
    ) {
        val percentInt = (percent * 100).toInt()
        if (percentInt < thresholdPercent) return

        // Only fire at exact threshold crossings (50, 80, 100)
        val level = when {
            percentInt >= 100 -> 100
            percentInt >= 80 -> 80
            percentInt >= 50 -> 50
            else -> return
        }

        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = context.getString(R.string.notification_cap_title, level)
        val body = context.getString(
            R.string.notification_cap_body,
            ByteFormatter.format(usedBytes),
            ByteFormatter.format(limitBytes),
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + level, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
