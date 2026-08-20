package com.dataguard.app.domain.usecase

import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.repository.DataCapRepository
import com.dataguard.app.domain.repository.DataUsageRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetTodayUsageUseCase @Inject constructor(
    private val repo: DataUsageRepository,
) {
    suspend operator fun invoke(): TodayUsage = repo.getTodayUsage()
}

class GetAppUsageUseCase @Inject constructor(
    private val repo: DataUsageRepository,
) {
    suspend operator fun invoke(start: Long, end: Long): List<AppUsage> =
        repo.getAppUsage(start, end)
}

class GetHistoryUseCase @Inject constructor(
    private val repo: DataUsageRepository,
) {
    suspend operator fun invoke(period: UsagePeriod): List<HistoryPoint> =
        repo.getHistory(period)
}

class ObserveCapUseCase @Inject constructor(
    private val repo: DataCapRepository,
) {
    operator fun invoke(): Flow<DataCap?> = repo.observeCap()
}

class SaveCapUseCase @Inject constructor(
    private val repo: DataCapRepository,
) {
    suspend operator fun invoke(cap: DataCap) = repo.saveCap(cap)
}

class RefreshDataUseCase @Inject constructor(
    private val repo: DataUsageRepository,
) {
    suspend operator fun invoke() = repo.refreshSnapshot()
}

class ComputeCapProgressUseCase @Inject constructor(
    private val capRepo: DataCapRepository,
    private val usageRepo: DataUsageRepository,
) {
    suspend operator fun invoke(): CapProgress? {
        val cap = capRepo.getCap() ?: return null
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        // Clamp the configured day into the current month's length.
        val startOfMonth = now.withDayOfMonth(1)
        val candidate = startOfMonth.withDayOfMonth(
            cap.cycleStartDay.coerceAtMost(startOfMonth.lengthOfMonth()),
        )
        val cycleStart = if (candidate.isAfter(now)) candidate.minusMonths(1) else candidate
        val cycleEnd = cycleStart.plusMonths(1)

        val startMillis = cycleStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = cycleEnd.atStartOfDay(zone).toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()

        val used = usageRepo.getUsageTotal(
            startMillis,
            minOf(nowMillis, endMillis),
            cap.networkType,
        )
        val limit = cap.monthlyLimitBytes
        val percent = if (limit > 0) (used.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
        val remaining = (limit - used).coerceAtLeast(0)

        // Simple linear projection based on average daily usage so far.
        val daysElapsed = (ChronoUnit.DAYS.between(cycleStart, now) + 1).coerceAtLeast(1)
        val predictedEndMillis = if (used > 0 && limit > used) {
            val avgPerDay = used.toDouble() / daysElapsed
            val daysLeft = (limit - used).toDouble() / avgPerDay
            nowMillis + (daysLeft * 24 * 60 * 60 * 1000).toLong()
        } else {
            null
        }

        return CapProgress(cap, startMillis, endMillis, used, remaining, percent, predictedEndMillis)
    }
}
