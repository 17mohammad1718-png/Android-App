package com.dataguard.app.domain.util

import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.NetworkType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataCapCalculatorTest {

    private val cap = DataCap(
        cycleStartDay = 31,
        monthlyLimitBytes = 10_000,
        alertThresholdPercent = 80,
        networkType = NetworkType.MOBILE,
    )

    @Test
    fun `cycle day is clamped independently for short months`() {
        val cycle = DataCapCalculator.cycleFor(
            cap = cap,
            now = LocalDate.of(2024, 3, 15),
            zone = ZoneOffset.UTC,
        )

        assertEquals(LocalDate.of(2024, 2, 29), dateOf(cycle.startMillis))
        assertEquals(LocalDate.of(2024, 3, 31), dateOf(cycle.endMillis))
    }

    @Test
    fun `cycle uses current month once cycle day has arrived`() {
        val cycle = DataCapCalculator.cycleFor(
            cap = cap,
            now = LocalDate.of(2024, 3, 31),
            zone = ZoneOffset.UTC,
        )

        assertEquals(LocalDate.of(2024, 3, 31), dateOf(cycle.startMillis))
        assertEquals(LocalDate.of(2024, 4, 30), dateOf(cycle.endMillis))
    }

    @Test
    fun `progress calculates remaining percentage and projection`() {
        val now = LocalDate.of(2024, 1, 5)
        val cycle = DataCapCalculator.cycleFor(
            cap = cap.copy(cycleStartDay = 1),
            now = now,
            zone = ZoneOffset.UTC,
        )
        val nowMillis = now.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val progress = DataCapCalculator.progress(
            cap = cap.copy(cycleStartDay = 1),
            cycle = cycle,
            usedBytes = 5_000,
            now = now,
            nowMillis = nowMillis,
            zone = ZoneOffset.UTC,
        )

        assertEquals(0.5f, progress.percent)
        assertEquals(5_000, progress.remainingBytes)
        assertEquals(nowMillis + 5L * 24 * 60 * 60 * 1000, progress.predictedEndMillis)
    }

    @Test
    fun `projection is absent when limit is exhausted`() {
        val now = LocalDate.of(2024, 1, 5)
        val cycle = DataCapCalculator.cycleFor(cap, now, ZoneOffset.UTC)
        val progress = DataCapCalculator.progress(
            cap = cap,
            cycle = cycle,
            usedBytes = cap.monthlyLimitBytes,
            now = now,
            nowMillis = 0,
            zone = ZoneOffset.UTC,
        )

        assertEquals(1f, progress.percent)
        assertEquals(0L, progress.remainingBytes)
        assertNull(progress.predictedEndMillis)
    }

    private fun dateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
