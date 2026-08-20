package com.dataguard.app.data.repository

import com.dataguard.app.data.local.dao.AppDailyAggregateDao
import com.dataguard.app.data.networkstats.NetworkStatsDataSource
import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.NetworkType
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.repository.SnapshotRepository
import com.dataguard.app.domain.util.DateUtils
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataUsageRepositoryImpl @Inject constructor(
    private val dataSource: NetworkStatsDataSource,
    private val snapshotRepository: SnapshotRepository,
    private val aggregateDao: AppDailyAggregateDao,
) : DataUsageRepository {

    override fun hasUsageAccess(): Boolean = dataSource.hasUsageAccess()

    override suspend fun getTodayUsage(): TodayUsage {
        val start = DateUtils.startOfDayMillis()
        val end = System.currentTimeMillis()
        return TodayUsage(
            wifiBytes = dataSource.queryWifiTotal(start, end),
            mobileBytes = dataSource.queryMobileTotal(start, end),
        )
    }

    override suspend fun getUsageTotal(start: Long, end: Long, networkType: NetworkType): Long =
        when (networkType) {
            NetworkType.WIFI -> dataSource.queryWifiTotal(start, end)
            NetworkType.MOBILE -> dataSource.queryMobileTotal(start, end)
            NetworkType.BOTH ->
                dataSource.queryWifiTotal(start, end) + dataSource.queryMobileTotal(start, end)
        }

    override suspend fun getAppUsage(start: Long, end: Long): List<AppUsage> =
        dataSource.queryAppUsage(start, end)
            .map { raw ->
                val (pkg, label) = dataSource.resolveApp(raw.uid)
                AppUsage(raw.uid, pkg, label, raw.wifiBytes, raw.mobileBytes)
            }
            .sortedByDescending { it.totalBytes }

    override suspend fun getHistory(period: UsagePeriod): List<HistoryPoint> {
        val days = when (period) {
            UsagePeriod.DAY -> 1
            UsagePeriod.WEEK -> 7
            UsagePeriod.MONTH -> 30
        }
        val fromEpochDay = LocalDate.now().minusDays((days - 1).toLong()).toEpochDay()
        return aggregateDao.dailyTotals(fromEpochDay).map { row ->
            HistoryPoint(row.date, DateUtils.label(row.date), row.wifiBytes, row.mobileBytes)
        }
    }

    override suspend fun refreshSnapshot() = snapshotRepository.captureAndStoreSnapshots()
}
