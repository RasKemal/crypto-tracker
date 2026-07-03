package com.example.stocktracker.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattersTest {

    @Test
    fun `formatUsd formats large prices with comma separators`() {
        assertEquals("$67,320.45", 67320.45.formatUsd())
    }

    @Test
    fun `formatUsd formats mid-range prices with 4 decimals`() {
        assertEquals("$0.5432", 0.5432.formatUsd())
    }

    @Test
    fun `formatUsd formats tiny prices with 6 decimals`() {
        assertEquals("$0.000042", 0.000042.formatUsd())
    }

    @Test
    fun `formatUsd formats zero`() {
        assertEquals("$0.000000", 0.0.formatUsd())
    }

    @Test
    fun `formatChangePercent positive`() {
        assertEquals("%2,45", 2.45.formatChangePercent())
    }

    @Test
    fun `formatChangePercent negative`() {
        assertEquals("-%1,30", (-1.3).formatChangePercent())
    }

    @Test
    fun `formatChangePercent zero`() {
        assertEquals("%0,00", 0.0.formatChangePercent())
    }

    @Test
    fun `formatUsdOrDash returns dash for null`() {
        assertEquals("--", null.formatUsdOrDash())
    }

    @Test
    fun `formatUsdOrDash returns formatted price for non-null`() {
        assertEquals("$1,234.56", 1234.56.formatUsdOrDash())
    }

    @Test
    fun `formatLargeUsd formats trillions`() {
        assertEquals("$1.50T", 1.5e12.formatLargeUsd())
    }

    @Test
    fun `formatLargeUsd formats billions`() {
        assertEquals("$2.30B", 2.3e9.formatLargeUsd())
    }
}
