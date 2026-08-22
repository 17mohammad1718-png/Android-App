package com.dataguard.app.domain.util

import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.DataCap
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Time boundaries for one billing cycle. End is exclusive. */
data class DataCapCycle(
    val startMillis: Long,
    val endMillis: Long,
)

/** Pure billing-cycle and projection calculations, kept separate for unit testing. */
object DataCapCalculator {

    fun cycleFor(
        cap: DataCap,
        now: LocalDate,
        zone: ZoneId,
    ): DataCapCycle {
        val currentMonth = YearMonth.from(now)
        val currentCandidate = cycleDate(currentMonth, cap.cycleStartDay)
        val startMonth = if (currentCandidate.isAfter(now)) {
            currentMonth.minusMonths(1)
        } else {
            currentMonth
        }
        val start = cycleDate(startMonth, cap.cycleStartDay)
        val end = cycleDate(startMonth.plusMonths(1), cap.cycleStartDay)
        return DataCapCycle(
            startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = end.atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }

    fun progress(
        cap: DataCap,
        cycle: DataCapCycle,
        usedBytes: Long,
        now: LocalDate,
        nowMillis: Long,
        zone: ZoneId,
    ): CapProgress {
        val limit = cap.monthlyLimitBytes
        val percent = if (limit > 0) {
            (usedBytes.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        val remaining = (limit - usedBytes).coerceAtLeast(0)
        val cycleStartDate = java.time.Instant.ofEpochMilli(cycle.startMillis)
            .atZone(zone)
            .toLocalDate()
        val daysElapsed = (ChronoUnit.DAYS.between(cycleStartDate, now) + 1).coerceAtLeast(1)
        val predictedEndMillis = if (usedBytes > 0 && limit > usedBytes) {
            // Uses fixed 24h days: DST shifts of up to an hour are deliberately
            // ignored — this is an estimate, not an exact deadline.
            val averagePerDay = usedBytes.toDouble() / daysElapsed
            val daysLeft = remaining.toDouble() / averagePerDay
            nowMillis + (daysLeft * MILLIS_PER_DAY).toLong()
        } else {
            null
        }

        return CapProgress(
            cap = cap,
            cycleStartMillis = cycle.startMillis,
            cycleEndMillis = cycle.endMillis,
            usedBytes = usedBytes,
            remainingBytes = remaining,
            percent = percent,
            predictedEndMillis = predictedEndMillis,
        )
    }

    private fun cycleDate(month: YearMonth, requestedDay: Int): LocalDate =
        month.atDay(requestedDay.coerceIn(1, month.lengthOfMonth()))

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
}
