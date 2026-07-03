package com.example.stocktracker.data.mapper

import com.example.stocktracker.data.local.entity.MarketAssetEntity
import com.example.stocktracker.data.local.entity.WatchlistEntity
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import com.example.stocktracker.domain.model.CryptoAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BinanceMappersTest {

    @Test
    fun `Ticker24hDto toDomain maps all fields`() {
        val dto = Ticker24hDto(
            symbol = "BTCUSDT",
            lastPrice = "67000.50",
            priceChangePercent = "2.45",
            highPrice = "68000.00",
            lowPrice = "66000.00",
            quoteVolume = "1500000000.00",
            weightedAvgPrice = "67500.00",
        )
        val result = dto.toDomain()
        assertEquals("BTCUSDT", result.id)
        assertEquals("BTC", result.symbol)
        assertEquals("USDT", result.quoteAsset)
        assertEquals(67000.50, result.priceUsd, 0.001)
        assertEquals(2.45, result.changePercent24Hr, 0.001)
        assertEquals(68000.0, result.high24Hr!!, 0.001)
        assertEquals(66000.0, result.low24Hr!!, 0.001)
    }

    @Test
    fun `toMarketEntity filters non-USDT pairs`() {
        val dto = Ticker24hDto(symbol = "BTCETH")
        assertNull(dto.toMarketEntity())
    }

    @Test
    fun `MarketAssetEntity toDomain preserves price data`() {
        val entity = MarketAssetEntity(
            id = "BTCUSDT", symbol = "BTC", name = "Bitcoin",
            priceUsd = 67000.0, changePercent24Hr = 2.5, volumeUsd24Hr = 1e9,
        )
        val domain = entity.toDomain()
        assertEquals("BTCUSDT", domain.id)
        assertEquals("BTC", domain.symbol)
        assertEquals("Bitcoin", domain.name)
        assertEquals(67000.0, domain.priceUsd, 0.001)
        assertEquals(2.5, domain.changePercent24Hr, 0.001)
        assertEquals("USDT", domain.quoteAsset)
    }

    @Test
    fun `WatchlistEntity toDomain has zero prices`() {
        val entity = WatchlistEntity(id = "ETHUSDT", symbol = "ETH", name = "Ethereum")
        val domain = entity.toDomain()
        assertEquals(0.0, domain.priceUsd, 0.001)
        assertEquals(0.0, domain.changePercent24Hr, 0.001)
    }

    @Test
    fun `CryptoAsset toWatchlistEntity maps id symbol name`() {
        val asset = CryptoAsset(
            id = "BTCUSDT", symbol = "BTC", name = "Bitcoin",
            quoteAsset = "USDT", priceUsd = 67000.0, changePercent24Hr = 2.5,
        )
        val entity = asset.toWatchlistEntity()
        assertEquals("BTCUSDT", entity.id)
        assertEquals("BTC", entity.symbol)
        assertEquals("Bitcoin", entity.name)
    }
}
