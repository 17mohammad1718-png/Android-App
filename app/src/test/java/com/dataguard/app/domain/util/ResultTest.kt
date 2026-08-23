package com.dataguard.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `Success wraps value correctly`() {
        val result = Result.Success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals(42, result.getOrNull())
        assertEquals(42, result.getOrDefault(0))
        assertNull(result.errorMessage())
    }

    @Test
    fun `Error wraps exception correctly`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals("test error", result.errorMessage())
        // Explicit Int type parameter: Error is Result<Nothing>, so the bare
        // literal would infer Nothing and fail to compile.
        val typed: Result<Int> = Result.Error(exception)
        assertEquals(0, typed.getOrDefault(0))
    }

    @Test
    fun `runCatching returns Success for successful block`() {
        val result = Result.runCatching { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching returns Error for throwing block`() {
        val result = Result.runCatching { throw IllegalStateException("boom") }
        assertTrue(result.isError)
        assertEquals("boom", result.errorMessage())
    }

    @Test
    fun `Error without message returns class name`() {
        val result = Result.Error(RuntimeException())
        assertEquals("RuntimeException", result.errorMessage())
    }
}
