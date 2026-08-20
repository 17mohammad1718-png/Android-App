package com.dataguard.app.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    private const val UNIQUE_NAME = "snapshot_worker"
    private const val INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        // NOTE: WorkManager periodic work has a 15-minute minimum interval.
        // No network constraint is required — the worker only reads local stats.
        // On Android 14+ / Doze the OS may defer this; the app also refreshes on
        // open to backfill any missed periods.
        val request = PeriodicWorkRequestBuilder<SnapshotWorker>(
            INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
