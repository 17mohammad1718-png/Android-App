package com.dataguard.app.domain.util

import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DataCapCalculatorExtendedTest {

    @Test
    fun `cycle starts in previous month when day not yet reached`() {
        val cap = DataCap(
            cycleStartDay = 15,
            monthlyLimitBytes = 10_000_000_000L,
            alertThresholdPercent = 80,
            networkType = NetworkType.MOBILE,
        )
        val cycle = DataCapCalculator.cycleFor(
            cap = cap,
            now = LocalDate.of(2024, 6, 10), // before 15th
            zone = ZoneOffset.UTC,
        )
        val startDate = java.time.Instant.ofEpochMilli(cycle.startMillis)
            .atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = java.time.Instant.ofEpochMilli(cycle.endMillis)
            .atZone(ZoneOffset.UTC).toLocalDate()

        assertEquals(LocalDate.of(2024, 5, 15), startDate)
        assertEquals(LocalDate.of(2024, 6, 15), endDate)
    }

    @Test
    fun `progress with zero usage gives zero percent`() {
        val cap = DataCap(
            cycleStartDay = 1,
            monthlyLimitBytes = 10_000_000_000L,
            alertThresholdPercent = 80,
            networkType = NetworkType.MOBILE,
        )
        val now = LocalDate.of(2024, 1, 15)
        val cycle = DataCapCalculator.cycleFor(cap, now, ZoneOffset.UTC)
        val nowMillis = now.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val progress = DataCapCalculator.progress(
            cap = cap,
            cycle = cycle,
            usedBytes = 0,
            now = now,
            nowMillis = nowMillis,
            zone = ZoneOffset.UTC,
        )

        assertEquals(0f, progress.percent)
        assertEquals(10_000_000_000L, progress.remainingBytes)
        assertNull(progress.predictedEndMillis)
    }

    @Test
    fun `progress with zero limit does not crash`() {
        val cap = DataCap(
            cycleStartDay = 1,
            monthlyLimitBytes = 0,
            alertThresholdPercent = 80,
            networkType = NetworkType.MOBILE,
        )
        val now = LocalDate.of(2024, 1, 15)
        val cycle = DataCapCalculator.cycleFor(cap, now, ZoneOffset.UTC)
        val nowMillis = now.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val progress = DataCapCalculator.progress(
            cap = cap,
            cycle = cycle,
            usedBytes = 5_000,
            now = now,
            nowMillis = nowMillis,
            zone = ZoneOffset.UTC,
        )

        assertEquals(0f, progress.percent)
        assertEquals(0L, progress.remainingBytes)
    }

    @Test
    fun `progress over limit clamps to 100 percent`() {
        val cap = DataCap(
            cycleStartDay = 1,
            monthlyLimitBytes = 10_000,
            alertThresholdPercent = 80,
            networkType = NetworkType.MOBILE,
        )
        val now = LocalDate.of(2024, 1, 15)
        val cycle = DataCapCalculator.cycleFor(cap, now, ZoneOffset.UTC)
        val nowMillis = now.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val progress = DataCapCalculator.progress(
            cap = cap,
            cycle = cycle,
            usedBytes = 20_000, // over limit
            now = now,
            nowMillis = nowMillis,
            zone = ZoneOffset.UTC,
        )

        assertEquals(1f, progress.percent)
        assertEquals(0L, progress.remainingBytes)
        // When usedBytes > limit, no prediction
        assertNull(progress.predictedEndMillis)
    }

    @Test
    fun `cycle handles February 29 in leap year`() {
        val cap = DataCap(
            cycleStartDay = 29,
            monthlyLimitBytes = 10_000,
            alertThresholdPercent = 80,
            networkType = NetworkType.BOTH,
        )
        val cycle = DataCapCalculator.cycleFor(
            cap = cap,
            now = LocalDate.of(2024, 2, 29), // Leap year
            zone = ZoneOffset.UTC,
        )
        val startDate = java.time.Instant.ofEpochMilli(cycle.startMillis)
            .atZone(ZoneOffset.UTC).toLocalDate()

        // Should start on Feb 29 in a leap year
        assertEquals(LocalDate.of(2024, 2, 29), startDate)
    }
}
