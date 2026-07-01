package com.example.stocktracker.ui.watchlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.R
import com.example.stocktracker.ui.util.mapLivePrice
import com.example.stocktracker.ui.util.toCryptoAssetUiModel
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WatchlistVM"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: CryptoRepository,
) : ViewModel() {

    private val _localQuery = MutableStateFlow("")
    private val _liveTicks = MutableStateFlow<Map<String, LivePrice>>(emptyMap())
    private val _quotes = MutableStateFlow<Map<String, CryptoAsset?>>(emptyMap())
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
                asset.toCryptoAssetUiModel()
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
                _quotes.update { it.filterKeys { key -> key in ids } }
                _livePrices.update { it.filterKeys { key -> key in ids } }
                _liveTicks.update { it.filterKeys { key -> key in ids } }
                items.forEach { asset ->
                    if (asset.id !in _quotes.value) fetchQuote(asset.id)
                    else syncLivePrice(asset.id)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.LocalQueryChanged -> _localQuery.value = event.query
            is WatchlistEvent.RemoveAsset -> removeAsset(event.id)
        }
    }

    private fun fetchQuote(id: String) {
        viewModelScope.launch {
            if (_quotes.value[id] != null) return@launch
            syncLivePrice(id)

            repository.getAsset(id)
                .onSuccess { asset ->
                    _quotes.update { it + (id to asset) }
                    syncLivePrice(id)
                }
                .onFailure { e ->
                    Log.w(TAG, "getAsset($id) failed: ${e.message}")
                    _quotes.update { it + (id to null) }
                    syncLivePrice(id)
                }
        }
    }

    private fun syncLivePrice(id: String) {
        if (watchlist.value.orEmpty().none { it.id == id }) return
        val tick = _liveTicks.value[id]
        val quote = _quotes.value[id]
        val quoteFailed = _quotes.value.containsKey(id) && quote == null
        _livePrices.update { prices ->
            prices + (id to mapLivePrice(
                tick = tick,
                quote = quote,
                quoteFailed = quoteFailed,
                isLoading = !_quotes.value.containsKey(id) && tick == null,
            ))
        }
    }

    private fun removeAsset(id: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(id)
            _quotes.update { it - id }
            _livePrices.update { it - id }
            _liveTicks.update { it - id }
        }
    }
}
