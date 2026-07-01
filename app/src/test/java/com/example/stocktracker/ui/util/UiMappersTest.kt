package com.example.stocktracker.ui.util

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiMappersTest {

    private val btcAsset = CryptoAsset(
        id = "BTCUSDT", symbol = "BTC", name = "Bitcoin",
        quoteAsset = "USDT", priceUsd = 67000.0, changePercent24Hr = 2.5,
    )

    @Test
    fun `toAssetUiModel maps fields correctly`() {
        val ui = btcAsset.toAssetUiModel(isInWatchlist = true)
        assertEquals("BTCUSDT", ui.id)
        assertEquals("BTC", ui.symbol)
        assertEquals("Bitcoin", ui.name)
        assertTrue(ui.isInWatchlist)
    }

    @Test
    fun `toAssetUiModel defaults isInWatchlist to false`() {
        assertFalse(btcAsset.toAssetUiModel().isInWatchlist)
    }

    @Test
    fun `mapLivePrice with tick uses tick price`() {
        val tick = LivePrice("BTCUSDT", 68000.0, 3.0)
        val result = mapLivePrice(tick = tick, quote = btcAsset)
        assertEquals(68000.0, result.priceUsd!!, 0.001)
        assertTrue(result.isPositive)
        assertFalse(result.isLoading)
    }

    @Test
    fun `mapLivePrice with tick uses tick changePercent when available`() {
        val tick = LivePrice("BTCUSDT", 68000.0, -1.5)
        val result = mapLivePrice(tick = tick, quote = btcAsset)
        assertFalse(result.isPositive)
    }

    @Test
    fun `mapLivePrice with tick falls back to quote changePercent when tick has none`() {
        val tick = LivePrice("BTCUSDT", 68000.0, changePercent24Hr = null)
        val result = mapLivePrice(tick = tick, quote = btcAsset)
        assertTrue(result.isPositive) // btcAsset.changePercent24Hr = 2.5
    }

    @Test
    fun `mapLivePrice with only quote uses quote data`() {
        val result = mapLivePrice(tick = null, quote = btcAsset)
        assertEquals(67000.0, result.priceUsd!!, 0.001)
        assertTrue(result.isPositive)
    }

    @Test
    fun `mapLivePrice with quoteFailed returns failed state`() {
        val result = mapLivePrice(tick = null, quote = null, quoteFailed = true)
        assertTrue(result.priceLoadFailed)
        assertEquals("--", result.formattedPrice)
    }

    @Test
    fun `mapLivePrice with isLoading returns loading state`() {
        val result = mapLivePrice(tick = null, quote = null, isLoading = true)
        assertEquals(PriceDisplayUiModel.Loading, result)
    }

    @Test
    fun `mapLivePrice with nothing returns dash state`() {
        val result = mapLivePrice(tick = null, quote = null)
        assertEquals("--", result.formattedPrice)
        assertFalse(result.isLoading)
        assertFalse(result.priceLoadFailed)
    }

    @Test
    fun `mapLivePrice tick takes priority over quote`() {
        val tick = LivePrice("BTCUSDT", 99999.0, 10.0)
        val result = mapLivePrice(tick = tick, quote = btcAsset)
        assertEquals(99999.0, result.priceUsd!!, 0.001)
    }
}
