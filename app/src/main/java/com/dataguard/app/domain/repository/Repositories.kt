package com.dataguard.app.domain.repository

import com.dataguard.app.domain.model.AppSettings
import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.DisplayUnit
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.NetworkType
import com.dataguard.app.domain.model.ThemeMode
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.model.UsagePeriod
import kotlinx.coroutines.flow.Flow

interface DataUsageRepository {
    fun hasUsageAccess(): Boolean

    suspend fun getTodayUsage(): TodayUsage

    suspend fun getUsageTotal(start: Long, end: Long, networkType: NetworkType): Long

    suspend fun getAppUsage(start: Long, end: Long): List<AppUsage>

    suspend fun getHistory(period: UsagePeriod): List<HistoryPoint>

    /** Trigger an immediate snapshot + backfill of the daily aggregates. */
    suspend fun refreshSnapshot()
}

interface DataCapRepository {
    fun observeCap(): Flow<DataCap?>

    suspend fun getCap(): DataCap?

    suspend fun saveCap(cap: DataCap)
}

interface SnapshotRepository {
    suspend fun captureAndStoreSnapshots()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDisplayUnit(unit: DisplayUnit)
}
