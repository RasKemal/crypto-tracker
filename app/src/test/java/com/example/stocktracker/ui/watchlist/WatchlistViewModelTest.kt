package com.example.stocktracker.ui.watchlist

import com.example.stocktracker.MainDispatcherRule
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: CryptoRepository = mockk()
    private val watchlistFlow = MutableStateFlow<List<CryptoAsset>>(emptyList())
    private val tickFlow = MutableSharedFlow<LivePrice>()

    private fun asset(id: String, price: Double = 1_000.0) = CryptoAsset(
        id = id, symbol = id.removeSuffix("USDT"), name = id,
        quoteAsset = "USDT", priceUsd = price, changePercent24Hr = 1.0,
    )

    @Before
    fun setup() {
        every { repository.getWatchlist() } returns watchlistFlow
        every { repository.observeLivePrices(any()) } returns tickFlow
    }

    @Test
    fun `asset with cached price shows price before any websocket tick`() = runTest {
        watchlistFlow.value = listOf(asset("BTCUSDT", price = 50_000.0))

        val viewModel = WatchlistViewModel(repository)

        val price = viewModel.livePrices.value["BTCUSDT"]
        assertNotNull(price)
        assertFalse(price!!.isLoading)
        assertEquals(50_000.0, price.priceUsd)
    }


    @Test
    fun `live tick updates livePrices`() = runTest {
        watchlistFlow.value = listOf(asset("BTCUSDT", price = 50_000.0))
        val viewModel = WatchlistViewModel(repository)

        tickFlow.emit(LivePrice(id = "BTCUSDT", price = 99_000.0))

        assertEquals(99_000.0, viewModel.livePrices.value["BTCUSDT"]?.priceUsd)
    }

    @Test
    fun `remove asset eagerly cleans up livePrices`() = runTest {
        watchlistFlow.value = listOf(asset("BTCUSDT", price = 50_000.0))
        coEvery { repository.removeFromWatchlist(any()) } returns Unit

        val viewModel = WatchlistViewModel(repository)
        assertTrue(viewModel.livePrices.value.containsKey("BTCUSDT"))

        viewModel.onEvent(WatchlistEvent.RemoveAsset("BTCUSDT"))

        assertFalse(viewModel.livePrices.value.containsKey("BTCUSDT"))
    }

    @Test
    fun `stale livePrices entry removed when watchlist shrinks`() = runTest {
        watchlistFlow.value = listOf(asset("BTCUSDT"), asset("ETHUSDT"))
        val viewModel = WatchlistViewModel(repository)
        assertTrue(viewModel.livePrices.value.containsKey("ETHUSDT"))

        watchlistFlow.value = listOf(asset("BTCUSDT"))

        assertFalse(viewModel.livePrices.value.containsKey("ETHUSDT"))
    }

}
