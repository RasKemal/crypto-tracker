package com.example.stocktracker.ui.search

import com.example.stocktracker.MainDispatcherRule
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.util.LoadState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: CryptoRepository = mockk()
    private val tickFlow = MutableSharedFlow<LivePrice>()

    private fun asset(id: String, symbol: String, price: Double = 1_000.0) = CryptoAsset(
        id = id, symbol = symbol, name = symbol,
        quoteAsset = "USDT", priceUsd = price, changePercent24Hr = 1.0,
    )

    @Before
    fun setup() {
        every { repository.getWatchlist() } returns flowOf(emptyList())
        every { repository.observeLivePrices(any()) } returns tickFlow
    }

    @Test
    fun `popular assets loaded on init`() = runTest {
        val assets = listOf(asset("BTCUSDT", "BTC"), asset("ETHUSDT", "ETH"))
        coEvery { repository.getPopularAssets() } returns Result.success(assets)

        val viewModel = SearchViewModel(repository)
        // UnconfinedTestDispatcher runs collect eagerly, activating WhileSubscribed
        val job = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        val content = viewModel.uiState.value.content
        assertTrue(content is LoadState.Success)
        assertEquals(2, (content as LoadState.Success).data.size)
        job.cancel()
    }

    @Test
    fun `query change after debounce triggers search`() = runTest {
        val assets = listOf(asset("BTCUSDT", "BTC"))
        coEvery { repository.getPopularAssets() } returns Result.success(assets)
        coEvery { repository.searchAssets("btc") } returns Result.success(assets)

        val viewModel = SearchViewModel(repository)
        viewModel.onEvent(SearchEvent.QueryChanged("btc"))
        advanceTimeBy(400L)

        coVerify { repository.searchAssets("btc") }
    }


    @Test
    fun `live tick updates livePrices for asset in current list`() = runTest {
        val assets = listOf(asset("BTCUSDT", "BTC", price = 50_000.0))
        coEvery { repository.getPopularAssets() } returns Result.success(assets)

        val viewModel = SearchViewModel(repository)
        assertNotNull(viewModel.livePrices.value["BTCUSDT"])

        tickFlow.emit(LivePrice(id = "BTCUSDT", price = 99_000.0))

        assertEquals(99_000.0, viewModel.livePrices.value["BTCUSDT"]?.priceUsd)
    }

    @Test
    fun `tick for asset not in current list is ignored`() = runTest {
        val assets = listOf(asset("BTCUSDT", "BTC"))
        coEvery { repository.getPopularAssets() } returns Result.success(assets)

        val viewModel = SearchViewModel(repository)
        tickFlow.emit(LivePrice(id = "SOLUSDT", price = 200.0))

        assertFalse(viewModel.livePrices.value.containsKey("SOLUSDT"))
    }
}
