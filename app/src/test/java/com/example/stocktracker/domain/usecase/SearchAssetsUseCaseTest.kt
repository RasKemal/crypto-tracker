package com.example.stocktracker.domain.usecase

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.repository.CryptoRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SearchAssetsUseCaseTest {

    private val repository: CryptoRepository = mockk()
    private val useCase = SearchAssetsUseCase(repository)

    private fun asset(symbol: String, name: String = symbol) = CryptoAsset(
        id = "${symbol}USDT", symbol = symbol, name = name,
        quoteAsset = "USDT", priceUsd = 100.0, changePercent24Hr = 1.0,
    )

    @Test
    fun `exact symbol match scores highest`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("BTCX", "BTCish"), asset("BTC", "Bitcoin"), asset("ABTC", "AlterBTC")),
        )
        val result = useCase("BTC").getOrThrow()
        assertEquals("BTC", result[0].symbol)
    }

    @Test
    fun `prefix match scores higher than contains`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("XBTC", "XBTC Coin"), asset("BTCX", "BTCish")),
        )
        val result = useCase("BTC").getOrThrow()
        assertEquals("BTCX", result[0].symbol)
    }

    @Test
    fun `name contains match is included`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("ETH", "Ethereum"), asset("XYZ", "XYZ Coin")),
        )
        val result = useCase("ether").getOrThrow()
        assertEquals(1, result.size)
        assertEquals("ETH", result[0].symbol)
    }

    @Test
    fun `blank query returns failure`() = runTest {
        val result = useCase("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun `limits results to 30`() = runTest {
        val assets = (1..50).map { asset("S$it", "Name$it") }
        coEvery { repository.getMarketSnapshot() } returns Result.success(assets)
        val result = useCase("S").getOrThrow()
        assertEquals(30, result.size)
    }

    @Test
    fun `query is case insensitive`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("BTC", "Bitcoin")),
        )
        val result = useCase("btc").getOrThrow()
        assertEquals(1, result.size)
    }

    @Test
    fun `no matches returns empty list`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("ETH", "Ethereum")),
        )
        val result = useCase("XYZ").getOrThrow()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.failure(IOException("no network"))
        val result = useCase("BTC")
        assertTrue(result.isFailure)
    }

    @Test
    fun `query is trimmed`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("BTC", "Bitcoin")),
        )
        val result = useCase("  btc  ").getOrThrow()
        assertEquals(1, result.size)
    }
}
