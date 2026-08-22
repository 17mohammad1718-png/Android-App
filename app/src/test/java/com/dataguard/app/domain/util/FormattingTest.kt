package com.dataguard.app.domain.util

import com.dataguard.app.domain.model.DisplayUnit
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
    fun `forced display unit is respected`() {
        // AUTO would pick KB here, but MB is forced.
        assertEquals("0.5 MB", ByteFormatter.format(512 * 1024, DisplayUnit.MB))
        // AUTO would pick GB here, but MB is forced.
        assertEquals("3072 MB", ByteFormatter.format(3L * 1024 * 1024 * 1024, DisplayUnit.MB))
        assertEquals("0.0 GB", ByteFormatter.format(1_536, DisplayUnit.GB))
        assertEquals("2.0 GB", ByteFormatter.format(2L * 1024 * 1024 * 1024, DisplayUnit.GB))
    }

    @Test
    fun `persian locale localizes digits decimal separator and units`() {
        Locale.setDefault(Locale("fa"))
        try {
            assertEquals("۱٫۵ کیلوبایت", ByteFormatter.format(1_536))
            assertEquals("۳٫۰ گیگابایت", ByteFormatter.format(3L * 1024 * 1024 * 1024))
            assertEquals("۲٫۰ مگابایت", ByteFormatter.format(2L * 1024 * 1024, DisplayUnit.MB))
        } finally {
            Locale.setDefault(Locale.US)
        }
    }

    @Test
    fun `day boundaries are end exclusive`() {
        val date = LocalDate.of(2024, 3, 10)
        val start = DateUtils.startOfDayMillis(date, ZoneOffset.UTC)
        val end = DateUtils.endOfDayMillis(date, ZoneOffset.UTC)

        assertEquals(24L * 60 * 60 * 1000, end - start)
    }
}
