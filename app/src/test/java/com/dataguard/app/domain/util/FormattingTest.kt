package com.dataguard.app.domain.util

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FormattingTest {

    private lateinit var originalLocale: Locale

    @Before
    fun rememberLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `byte formatter selects compact binary unit`() {
        assertEquals("512 B", ByteFormatter.format(512))
        assertEquals("1.5 KB", ByteFormatter.format(1_536))
        assertEquals("2.0 MB", ByteFormatter.format(2L * 1024 * 1024))
        assertEquals("3.0 GB", ByteFormatter.format(3L * 1024 * 1024 * 1024))
    }

    @Test
    fun `day boundaries are end exclusive`() {
        val date = LocalDate.of(2024, 3, 10)
        val start = DateUtils.startOfDayMillis(date, ZoneOffset.UTC)
        val end = DateUtils.endOfDayMillis(date, ZoneOffset.UTC)

        assertEquals(24L * 60 * 60 * 1000, end - start)
    }
}
