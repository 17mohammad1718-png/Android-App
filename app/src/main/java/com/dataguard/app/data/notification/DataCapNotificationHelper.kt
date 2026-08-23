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
 *
 * Crossing detection is persisted in SharedPreferences so each threshold
 * notifies at most ONCE per billing cycle (Issue #5 acceptance criteria):
 * - key = cycle start epoch-day, value = highest level already notified
 * - when the stored cycle differs from the current one, state resets
 */
@Singleton
class DataCapNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "data_cap_alerts"
        private const val NOTIFICATION_ID_BASE = 9000
        private const val PREFS = "data_cap_alerts"
        private const val KEY_CYCLE = "cycle_start_epoch_day"
        private const val KEY_LEVEL = "notified_level"
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
     * Send a threshold notification if [percent] crosses a 50/80/100 level that
     * has not been notified yet in this billing cycle ([cycleStartEpochDay]).
     *
     * @return true when a notification was actually posted.
     */
    fun notifyThresholdIfNeeded(
        percent: Float,
        thresholdPercent: Int,
        usedBytes: Long,
        limitBytes: Long,
        cycleStartEpochDay: Long,
    ): Boolean {
        val percentInt = (percent * 100).toInt()
        if (percentInt < thresholdPercent) return false

        // Only fire at exact threshold crossings (50, 80, 100)
        val level = when {
            percentInt >= 100 -> 100
            percentInt >= 80 -> 80
            percentInt >= 50 -> 50
            else -> return false
        }

        // Persisted crossing detection: reset on cycle rollover.
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedCycle = prefs.getLong(KEY_CYCLE, -1L)
        val notifiedLevel = if (storedCycle == cycleStartEpochDay) prefs.getInt(KEY_LEVEL, 0) else 0
        if (storedCycle != cycleStartEpochDay) {
            prefs.edit().clear()
                .putLong(KEY_CYCLE, cycleStartEpochDay)
                .putInt(KEY_LEVEL, 0)
                .apply()
        }
        if (level <= notifiedLevel) return false

        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, level, intent,
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

        prefs.edit().putInt(KEY_LEVEL, level).apply()
        return true
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
