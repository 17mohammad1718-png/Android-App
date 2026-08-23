package com.dataguard.app.presentation.screens.dashboard

import com.dataguard.app.domain.model.AppUsage
import com.dataguard.app.domain.model.CapProgress
import com.dataguard.app.domain.model.DataCap
import com.dataguard.app.domain.model.HistoryPoint
import com.dataguard.app.domain.model.NetworkType
import com.dataguard.app.domain.model.TodayUsage
import com.dataguard.app.domain.model.UsagePeriod
import com.dataguard.app.domain.repository.DataUsageRepository
import com.dataguard.app.domain.usecase.ComputeCapProgressUseCase
import com.dataguard.app.domain.usecase.GetTodayUsageUseCase
import com.dataguard.app.domain.usecase.RefreshDataUseCase
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake implementations for unit testing ViewModels. */

class FakeDataUsageRepository(
    private var hasAccess: Boolean = true,
    private var todayUsage: TodayUsage = TodayUsage(wifiBytes = 1000, mobileBytes = 500),
    private var shouldThrow: Boolean = false,
) : DataUsageRepository {

    var refreshCalled = false
        private set

    override fun hasUsageAccess(): Boolean = hasAccess

    override suspend fun getTodayUsage(): TodayUsage {
        if (shouldThrow) throw RuntimeException("test error")
        return todayUsage
    }

    override suspend fun getUsageTotal(start: Long, end: Long, networkType: NetworkType): Long = 0

    override suspend fun getAppUsage(start: Long, end: Long): List<AppUsage> = emptyList()

    override suspend fun getHistory(period: UsagePeriod): List<HistoryPoint> = emptyList()

    override suspend fun refreshSnapshot() {
        if (shouldThrow) throw RuntimeException("test error")
        refreshCalled = true
    }
}

class FakeCapProgressUseCase(
    private var result: CapProgress? = null,
) {
    suspend operator fun invoke(): CapProgress? = result

    fun setResult(value: CapProgress?) {
        result = value
    }
}

class DashboardViewModelTest {

    @Test
    fun `initial state is correct defaults`() {
        val state = DashboardUiState()
        assertNull(state.todayUsage)
        assertNull(state.capProgress)
        assertFalse(state.isRefreshing)
        assertTrue(state.hasAccess)
        assertNull(state.error)
    }

    @Test
    fun `state update copies correctly`() {
        val state = DashboardUiState()
        val updated = state.copy(
            todayUsage = TodayUsage(2000, 1000),
            isRefreshing = false,
        )
        assertNotNull(updated.todayUsage)
        assertEquals(3000L, updated.todayUsage!!.totalBytes)
        assertFalse(updated.isRefreshing)
    }
}
