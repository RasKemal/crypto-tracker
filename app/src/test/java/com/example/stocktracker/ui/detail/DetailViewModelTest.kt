package com.example.stocktracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.example.stocktracker.MainDispatcherRule
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.navigation.AppDestination
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: CryptoRepository = mockk()
    private val tickFlow = MutableSharedFlow<LivePrice>()

    private val savedStateHandle = SavedStateHandle(
        mapOf(
            AppDestination.Detail.ARG_ID to "BTCUSDT",
            AppDestination.Detail.ARG_SYMBOL to "BTC",
            AppDestination.Detail.ARG_NAME to "Bitcoin",
        ),
    )

    private val btcAsset = CryptoAsset(
        id = "BTCUSDT", symbol = "BTC", name = "Bitcoin",
        quoteAsset = "USDT", priceUsd = 67_000.0, changePercent24Hr = 2.5,
    )

    @Before
    fun setup() {
        every { repository.getWatchlist() } returns flowOf(emptyList())
        every { repository.observeLivePrices(any()) } returns tickFlow
    }

    private fun createViewModel() = DetailViewModel(savedStateHandle, repository)

    @Test
    fun `successful load populates uiState with asset data`() = runTest {
        coEvery { repository.getAsset("BTCUSDT") } returns Result.success(btcAsset)

        val viewModel = createViewModel()
        val job = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        val uiState = viewModel.uiState.value
        assertEquals("BTC", uiState.symbol)
        assertEquals("Bitcoin", uiState.name)
        assertTrue(uiState.content is LoadState.Success)
        job.cancel()
    }

    @Test
    fun `first load failure shows error state`() = runTest {
        coEvery { repository.getAsset("BTCUSDT") } returns Result.failure(IOException())

        val viewModel = createViewModel()
        val job = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.value.content is LoadState.Error)
        job.cancel()
    }
    

    @Test
    fun `live tick updates livePrice`() = runTest {
        coEvery { repository.getAsset("BTCUSDT") } returns Result.success(btcAsset)

        val viewModel = createViewModel()
        tickFlow.emit(LivePrice(id = "BTCUSDT", price = 99_000.0))

        assertEquals(99_000.0, viewModel.livePrice.value.priceUsd)
    }

    @Test
    fun `toggle watchlist adds asset when not bookmarked`() = runTest {
        coEvery { repository.getAsset("BTCUSDT") } returns Result.success(btcAsset)
        coEvery { repository.addToWatchlist(any()) } returns Unit

        val viewModel = createViewModel()
        viewModel.onEvent(DetailEvent.ToggleWatchlist)

        coVerify { repository.addToWatchlist(any()) }
    }

    @Test
    fun `toggle watchlist removes asset when already bookmarked`() = runTest {
        coEvery { repository.getAsset("BTCUSDT") } returns Result.success(btcAsset)
        every { repository.getWatchlist() } returns flowOf(listOf(btcAsset))
        coEvery { repository.removeFromWatchlist("BTCUSDT") } returns Unit

        val viewModel = createViewModel()
        val job = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.isInWatchlist)

        viewModel.onEvent(DetailEvent.ToggleWatchlist)

        coVerify { repository.removeFromWatchlist("BTCUSDT") }
        job.cancel()
    }
}
