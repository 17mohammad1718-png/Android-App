package com.dataguard.app.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dataguard.app.data.notification.DataCapNotificationHelper
import com.dataguard.app.domain.repository.SnapshotRepository
import com.dataguard.app.domain.usecase.ComputeCapProgressUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SnapshotWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val snapshotRepository: SnapshotRepository,
    private val computeCapProgress: ComputeCapProgressUseCase,
    private val notificationHelper: DataCapNotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SnapshotWorker"
    }

    override suspend fun doWork(): Result = try {
        snapshotRepository.captureAndStoreSnapshots()
        // After capturing data, check if a data cap alert should fire.
        checkCapNotification()
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "doWork failed", e)
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    private suspend fun checkCapNotification() {
        try {
            val progress = computeCapProgress() ?: return
            notificationHelper.notifyThresholdIfNeeded(
                percent = progress.percent,
                thresholdPercent = progress.cap.alertThresholdPercent,
                usedBytes = progress.usedBytes,
                limitBytes = progress.cap.monthlyLimitBytes,
            )
        } catch (e: Exception) {
            Log.w(TAG, "checkCapNotification failed", e)
        }
    }
}
