package com.dataguard.app.data.repository

import com.dataguard.app.data.local.dao.AppDailyAggregateDao
import com.dataguard.app.data.local.dao.UsageSnapshotDao
import com.dataguard.app.data.local.entity.AppDailyAggregateEntity
import com.dataguard.app.data.local.entity.UsageSnapshotEntity
import com.dataguard.app.data.networkstats.NetworkStatsDataSource
import com.dataguard.app.domain.repository.SnapshotRepository
import com.dataguard.app.domain.util.DateUtils
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepositoryImpl @Inject constructor(
    private val dataSource: NetworkStatsDataSource,
    private val aggregateDao: AppDailyAggregateDao,
    private val snapshotDao: UsageSnapshotDao,
) : SnapshotRepository {

    companion object {
        /** How many recent days to (re)compute on each run, to backfill gaps. */
        private const val BACKFILL_DAYS = 3

        /**
         * The worker runs every ~15 minutes, but a full raw audit snapshot is only
         * recorded at most once in this interval. Without this the append-only
         * `usage_snapshot` table would grow by ~96 full snapshots per day.
         */
        private const val RAW_SNAPSHOT_MIN_INTERVAL_MS = 12L * 60L * 60L * 1000

        /** Raw audit snapshots older than this are pruned on every run. */
        private const val RAW_SNAPSHOT_RETENTION_MS = 30L * 24L * 60L * 60L * 1000
    }

    override suspend fun captureAndStoreSnapshots() {
        if (!dataSource.hasUsageAccess()) return
        val now = System.currentTimeMillis()
        snapshotDao.deleteOlderThan(now - RAW_SNAPSHOT_RETENTION_MS)
        val latest = snapshotDao.latestTimestamp()
        val recordRawSnapshot = latest == null || now - latest >= RAW_SNAPSHOT_MIN_INTERVAL_MS
        val today = LocalDate.now()
        for (daysAgo in 0 until BACKFILL_DAYS) {
            captureDay(today.minusDays(daysAgo.toLong()), recordRawSnapshot)
        }
    }

    private suspend fun captureDay(day: LocalDate, recordRawSnapshot: Boolean) {
        val start = DateUtils.startOfDayMillis(day)
        val end = DateUtils.endOfDayMillis(day)
        val raw = dataSource.queryAppUsage(start, end)

        // Resolve each UID to a package once and reuse for both outputs below.
        val resolved = raw.map { r -> r to dataSource.resolveApp(r.uid) }

        // Upsert daily per-app totals (primary key: date + packageName).
        val aggregates = resolved.map { (r, pkgLabel) ->
            AppDailyAggregateEntity(
                date = day.toEpochDay(),
                appPackageName = pkgLabel.first,
                totalWifiBytes = r.wifiBytes,
                totalMobileBytes = r.mobileBytes,
            )
        }
        if (aggregates.isNotEmpty()) aggregateDao.upsertAll(aggregates)

        // Raw snapshot is only recorded for "today", as an audit log, and at most
        // once per RAW_SNAPSHOT_MIN_INTERVAL_MS (see captureAndStoreSnapshots).
        if (recordRawSnapshot && day == LocalDate.now()) {
            val now = System.currentTimeMillis()
            val snapshots = resolved.map { (r, pkgLabel) ->
                UsageSnapshotEntity(
                    timestamp = now,
                    appPackageName = pkgLabel.first,
                    appName = pkgLabel.second,
                    wifiBytesReceived = r.wifiRx,
                    wifiBytesSent = r.wifiTx,
                    mobileBytesReceived = r.mobileRx,
                    mobileBytesSent = r.mobileTx,
                    periodStart = start,
                    periodEnd = end,
                )
            }
            if (snapshots.isNotEmpty()) snapshotDao.insertAll(snapshots)
        }
    }
}
