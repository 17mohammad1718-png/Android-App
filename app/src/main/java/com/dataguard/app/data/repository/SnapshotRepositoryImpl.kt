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
    }

    override suspend fun captureAndStoreSnapshots() {
        if (!dataSource.hasUsageAccess()) return
        val today = LocalDate.now()
        for (daysAgo in 0 until BACKFILL_DAYS) {
            captureDay(today.minusDays(daysAgo.toLong()))
        }
    }

    private suspend fun captureDay(day: LocalDate) {
        val start = DateUtils.startOfDayMillis(day)
        val end = DateUtils.endOfDayMillis(day)
        val raw = dataSource.queryAppUsage(start, end)

        // Upsert daily per-app totals (primary key: date + packageName).
        val aggregates = raw.map { r ->
            val (pkg, _) = dataSource.resolveApp(r.uid)
            AppDailyAggregateEntity(
                date = day.toEpochDay(),
                appPackageName = pkg,
                totalWifiBytes = r.wifiBytes,
                totalMobileBytes = r.mobileBytes,
            )
        }
        if (aggregates.isNotEmpty()) aggregateDao.upsertAll(aggregates)

        // Raw snapshot is only recorded for "today", as an audit log.
        if (day == LocalDate.now()) {
            val snapshots = raw.map { r ->
                val (pkg, label) = dataSource.resolveApp(r.uid)
                UsageSnapshotEntity(
                    timestamp = System.currentTimeMillis(),
                    appPackageName = pkg,
                    appName = label,
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
