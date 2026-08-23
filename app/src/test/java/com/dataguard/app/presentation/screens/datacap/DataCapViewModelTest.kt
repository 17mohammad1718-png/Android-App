package com.dataguard.app.presentation.screens.datacap

import com.dataguard.app.domain.model.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DataCapViewModelTest {

    @Test
    fun `initial state has correct defaults`() {
        val state = DataCapUiState()
        assertEquals(1, state.cycleDay)
        assertEquals("10", state.limitText)
        assertEquals(80, state.threshold)
        assertEquals(NetworkType.MOBILE, state.networkType)
        assertFalse(state.saved)
        assertFalse(state.invalidLimit)
        assertFalse(state.saveFailed)
    }

    @Test
    fun `state updates cycleDay correctly`() {
        val state = DataCapUiState()
        val updated = state.copy(cycleDay = 15, saved = false, saveFailed = false)
        assertEquals(15, updated.cycleDay)
        assertFalse(updated.saved)
        assertFalse(updated.saveFailed)
    }

    @Test
    fun `state updates limitText correctly`() {
        val state = DataCapUiState()
        val updated = state.copy(
            limitText = "20",
            saved = false,
            saveFailed = false,
            invalidLimit = false,
        )
        assertEquals("20", updated.limitText)
        assertFalse(updated.invalidLimit)
    }

    @Test
    fun `state updates threshold correctly`() {
        val state = DataCapUiState()
        val updated = state.copy(threshold = 90, saved = false, saveFailed = false)
        assertEquals(90, updated.threshold)
    }

    @Test
    fun `state updates networkType correctly`() {
        val state = DataCapUiState()
        val updated = state.copy(networkType = NetworkType.BOTH, saved = false, saveFailed = false)
        assertEquals(NetworkType.BOTH, updated.networkType)
    }
}
