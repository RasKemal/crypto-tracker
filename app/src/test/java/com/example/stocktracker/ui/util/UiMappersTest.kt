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
    fun `toCryptoAssetUiModel maps fields correctly`() {
        val ui = btcAsset.toCryptoAssetUiModel(isInWatchlist = true)
        assertEquals("BTCUSDT", ui.id)
        assertEquals("BTC", ui.symbol)
        assertEquals("Bitcoin", ui.name)
        assertTrue(ui.isInWatchlist)
    }

    @Test
    fun `mapLivePrice with tick uses tick price`() {
        val tick = LivePrice("BTCUSDT", 68000.0, 3.0)
        val result = mapLivePrice(tick = tick, asset =btcAsset)
        assertEquals(68000.0, result.priceUsd!!, 0.001)
        assertTrue(result.isPositive)
        assertFalse(result.isLoading)
    }

    @Test
    fun `mapLivePrice with tick falls back to asset changePercent when tick has none`() {
        val tick = LivePrice("BTCUSDT", 68000.0, changePercent24Hr = null)
        val result = mapLivePrice(tick = tick, asset =btcAsset)
        assertTrue(result.isPositive) // btcAsset.changePercent24Hr = 2.5
    }

    @Test
    fun `mapLivePrice with only asset uses asset data`() {
        val result = mapLivePrice(tick = null, asset =btcAsset)
        assertEquals(67000.0, result.priceUsd!!, 0.001)
        assertTrue(result.isPositive)
    }

    @Test
    fun `mapLivePrice with isLoading returns loading state`() {
        val result = mapLivePrice(tick = null, asset =null, isLoading = true)
        assertEquals(PriceDisplayUiModel.Loading, result)
    }
}
