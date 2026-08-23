package com.dataguard.app.data.networkstats

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUsageRawTest {

    @Test
    fun `initial values are zero`() {
        val raw = AppUsageRaw(uid = 1000)
        assertEquals(0L, raw.wifiRx)
        assertEquals(0L, raw.wifiTx)
        assertEquals(0L, raw.mobileRx)
        assertEquals(0L, raw.mobileTx)
        assertEquals(0L, raw.wifiBytes)
        assertEquals(0L, raw.mobileBytes)
        assertEquals(0L, raw.totalBytes)
    }

    @Test
    fun `withWifi accumulates correctly`() {
        val raw = AppUsageRaw(uid = 1000)
            .withWifi(rx = 100, tx = 50)
            .withWifi(rx = 200, tx = 100)

        assertEquals(300L, raw.wifiRx)
        assertEquals(150L, raw.wifiTx)
        assertEquals(450L, raw.wifiBytes)
        assertEquals(0L, raw.mobileBytes)
        assertEquals(450L, raw.totalBytes)
    }

    @Test
    fun `withMobile accumulates correctly`() {
        val raw = AppUsageRaw(uid = 1000)
            .withMobile(rx = 500, tx = 200)
            .withMobile(rx = 300, tx = 100)

        assertEquals(800L, raw.mobileRx)
        assertEquals(300L, raw.mobileTx)
        assertEquals(1100L, raw.mobileBytes)
        assertEquals(0L, raw.wifiBytes)
        assertEquals(1100L, raw.totalBytes)
    }

    @Test
    fun `combined wifi and mobile totals`() {
        val raw = AppUsageRaw(uid = 1000)
            .withWifi(rx = 100, tx = 50)
            .withMobile(rx = 200, tx = 100)

        assertEquals(150L, raw.wifiBytes)
        assertEquals(300L, raw.mobileBytes)
        assertEquals(450L, raw.totalBytes)
    }

    @Test
    fun `immutability is preserved`() {
        val original = AppUsageRaw(uid = 1000)
        val modified = original.withWifi(rx = 100, tx = 50)

        // Original should remain unchanged
        assertEquals(0L, original.wifiRx)
        assertEquals(0L, original.wifiTx)
        assertEquals(0L, original.totalBytes)

        // Modified should have the new values
        assertEquals(100L, modified.wifiRx)
        assertEquals(50L, modified.wifiTx)
        assertEquals(150L, modified.totalBytes)
    }
}
