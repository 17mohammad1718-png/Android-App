package com.dataguard.app.presentation.screens.applist

import com.dataguard.app.domain.model.UsagePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppListViewModelTest {

    @Test
    fun `initial state has correct defaults`() {
        val state = AppListUiState()
        assertEquals(UsagePeriod.DAY, state.period)
        assertEquals(emptyList<Nothing>(), state.items)
        assertFalse(state.loading)
        assertFalse(state.error)
    }

    @Test
    fun `state updates period correctly`() {
        val state = AppListUiState()
        val updated = state.copy(period = UsagePeriod.WEEK)
        assertEquals(UsagePeriod.WEEK, updated.period)
    }

    @Test
    fun `loading state transitions correctly`() {
        val initial = AppListUiState()
        val loading = initial.copy(loading = true, error = false)
        assertEquals(true, loading.loading)
        assertEquals(false, loading.error)

        val loaded = loading.copy(loading = false, error = false)
        assertEquals(false, loaded.loading)
    }
}
