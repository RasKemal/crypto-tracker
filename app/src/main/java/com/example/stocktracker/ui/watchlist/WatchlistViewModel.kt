package com.example.stocktracker.ui.watchlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.R
import com.example.stocktracker.ui.util.mapLivePrice
import com.example.stocktracker.ui.util.toAssetUiModel
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WatchlistVM"
private const val QUOTE_REFRESH_INTERVAL_MS = 60_000L

private data class QuoteFetchState(
    val quote: CryptoAsset? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: CryptoRepository,
) : ViewModel() {

    private val _localQuery = MutableStateFlow("")
    private val _liveTicks = MutableStateFlow<Map<String, LivePrice>>(emptyMap())
    private val _quoteStates = MutableStateFlow<Map<String, QuoteFetchState>>(emptyMap())
    private val _bannerMessageRes = MutableStateFlow<Int?>(null)
    private val _livePrices = MutableStateFlow<Map<String, PriceDisplayUiModel>>(emptyMap())

    val livePrices: StateFlow<Map<String, PriceDisplayUiModel>> = _livePrices

    private val watchlist: StateFlow<List<CryptoAsset>?> = repository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlist, _localQuery, _bannerMessageRes,
    ) { assets, query, bannerRes ->
        if (assets == null) return@combine WatchlistUiState(isLoading = true)

        val filtered = if (query.isBlank()) assets
        else assets.filter {
            it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
        WatchlistUiState(
            isLoading = false,
            localQuery = query,
            items = filtered.map { asset ->
                asset.toAssetUiModel()
            },
            isEmpty = assets.isEmpty(),
            bannerMessageRes = bannerRes,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WatchlistUiState(),
    )

    init {
        watchlist
            .map { it.orEmpty().map(CryptoAsset::id) }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf<LivePrice>()
                else repository.observeLivePrices(ids)
            }
            .catch {
                _bannerMessageRes.value = R.string.banner_live_feed_disconnected
            }
            .onEach {
                _bannerMessageRes.value = null
                _liveTicks.update { map -> map + (it.id to it) }
                syncLivePrice(it.id)
            }
            .launchIn(viewModelScope)

        watchlist
            .onEach { items ->
                if (items == null) return@onEach
                val ids = items.map(CryptoAsset::id).toSet()
                _quoteStates.update { states -> states.filterKeys { it in ids } }
                _livePrices.update { prices -> prices.filterKeys { it in ids } }
                _liveTicks.update { ticks -> ticks.filterKeys { it in ids } }
                items.forEach { asset ->
                    if (asset.id !in _quoteStates.value) fetchQuote(asset.id)
                    else syncLivePrice(asset.id)
                }
            }
            .launchIn(viewModelScope)

        watchlist
            .map { it.orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { items ->
                if (items.isEmpty()) flowOf()
                else tickerRefreshFlow(items.map(CryptoAsset::id))
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.LocalQueryChanged -> _localQuery.value = event.query
            is WatchlistEvent.RemoveAsset -> removeAsset(event.id)
        }
    }

    private fun tickerRefreshFlow(ids: List<String>) = flow {
        while (true) {
            delay(QUOTE_REFRESH_INTERVAL_MS)
            ids.forEach { fetchQuote(it, isRefresh = true) }
            emit(Unit)
        }
    }

    private fun fetchQuote(id: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            val current = _quoteStates.value[id]
            if (current?.isLoading == true) return@launch
            if (!isRefresh && current?.quote != null) return@launch

            _quoteStates.update { it + (id to QuoteFetchState(isLoading = true)) }
            syncLivePrice(id)

            repository.getAsset(id)
                .onSuccess { asset ->
                    _quoteStates.update { it + (id to QuoteFetchState(quote = asset)) }
                    syncLivePrice(id)
                }
                .onFailure { e ->
                    Log.w(TAG, "getAsset($id) failed: ${e.message}")
                    _quoteStates.update { states ->
                        val previous = states[id]
                        states + (id to QuoteFetchState(
                            quote = previous?.quote,
                            failed = previous?.quote == null,
                        ))
                    }
                    syncLivePrice(id)
                    if (isRefresh) {
                        _bannerMessageRes.value = R.string.banner_prices_partial_update
                    }
                }
        }
    }

    private fun syncLivePrice(id: String) {
        if (watchlist.value.orEmpty().none { it.id == id }) return
        val quoteState = _quoteStates.value[id]
        val quoteFailed = quoteState?.failed == true && quoteState.quote == null
        val tick = _liveTicks.value[id]
        val quote = quoteState?.quote
        _livePrices.update { prices ->
            prices + (id to mapLivePrice(
                tick = tick,
                quote = quote,
                quoteFailed = quoteFailed,
                isLoading = quote == null && tick == null && !quoteFailed,
            ))
        }
    }

    private fun removeAsset(id: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(id)
            _quoteStates.update { it - id }
            _livePrices.update { it - id }
            _liveTicks.update { it - id }
        }
    }
}
