package com.dataguard.app.presentation.screens.history

import com.dataguard.app.domain.model.UsagePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HistoryViewModelTest {

    @Test
    fun `initial state has correct defaults`() {
        val state = HistoryUiState()
        assertEquals(UsagePeriod.WEEK, state.period)
        assertEquals(emptyList<Nothing>(), state.points)
        assertFalse(state.loading)
        assertFalse(state.error)
    }

    @Test
    fun `period change updates state`() {
        val state = HistoryUiState()
        val updated = state.copy(period = UsagePeriod.MONTH)
        assertEquals(UsagePeriod.MONTH, updated.period)
    }
}
