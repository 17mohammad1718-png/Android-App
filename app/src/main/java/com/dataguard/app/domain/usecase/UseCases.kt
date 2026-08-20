package com.dataguard.app.domain.usecase

import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.repository.DataCapRepository
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.util.DataCapCalculator
import java.time.LocalDate
import java.time.ZoneId
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

        val cycle = DataCapCalculator.cycleFor(cap, now, zone)
        val nowMillis = System.currentTimeMillis()
        val used = usageRepo.getUsageTotal(
            cycle.startMillis,
            minOf(nowMillis, cycle.endMillis),
            cap.networkType,
        )
        return DataCapCalculator.progress(cap, cycle, used, now, nowMillis, zone)
    }
}
