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

class GetPopularCryptosUseCaseTest {

    private val repository: CryptoRepository = mockk()
    private val useCase = GetPopularCryptosUseCase(repository)

    private fun asset(symbol: String, volume: Double?) = CryptoAsset(
        id = "${symbol}USDT", symbol = symbol, name = symbol,
        quoteAsset = "USDT", priceUsd = 100.0, changePercent24Hr = 1.0,
        volumeUsd24Hr = volume,
    )

    @Test
    fun `filters out assets with zero volume`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("BTC", 1e9), asset("DEAD", 0.0), asset("ETH", 5e8)),
        )
        val result = useCase().getOrThrow()
        assertEquals(2, result.size)
        assertEquals("BTC", result[0].symbol)
        assertEquals("ETH", result[1].symbol)
    }

    @Test
    fun `filters out assets with null volume`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("BTC", 1e9), asset("NULL", null)),
        )
        val result = useCase().getOrThrow()
        assertEquals(1, result.size)
    }

    @Test
    fun `limits to 10 results`() = runTest {
        val assets = (1..20).map { asset("C$it", it * 1000.0) }
        coEvery { repository.getMarketSnapshot() } returns Result.success(assets)
        val result = useCase().getOrThrow()
        assertEquals(10, result.size)
    }

    @Test
    fun `returns empty list when all volumes are zero`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.success(
            listOf(asset("A", 0.0), asset("B", 0.0)),
        )
        val result = useCase().getOrThrow()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        coEvery { repository.getMarketSnapshot() } returns Result.failure(IOException("no network"))
        val result = useCase()
        assertTrue(result.isFailure)
    }
}
