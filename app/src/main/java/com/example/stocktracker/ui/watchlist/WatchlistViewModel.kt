package com.example.stocktracker.ui.watchlist

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.model.WatchlistItemUiModel
import com.example.stocktracker.ui.model.toWatchlistItemUiModel
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

@Immutable
data class WatchlistUiState(
    val localQuery: String = "",
    val items: List<WatchlistItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

sealed interface WatchlistEvent {
    data class LocalQueryChanged(val query: String) : WatchlistEvent
    data class RemoveAsset(val id: String) : WatchlistEvent
    data class AssetClicked(val item: WatchlistItemUiModel) : WatchlistEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: CryptoRepository,
) : ViewModel() {

    private val _localQuery = MutableStateFlow("")

    private val watchlist: StateFlow<List<CryptoAsset>> = repository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _liveTicks = MutableStateFlow<Map<String, LivePrice>>(emptyMap())
    private val _quoteCache = MutableStateFlow<Map<String, CryptoAsset>>(emptyMap())

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlist, _liveTicks, _quoteCache, _localQuery,
    ) { assets, ticks, quotes, query ->
        val filtered = if (query.isBlank()) assets
        else assets.filter {
            it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
        WatchlistUiState(
            localQuery = query,
            items = filtered.mapIndexed { index, asset ->
                asset.toWatchlistItemUiModel(
                    rank = index + 1,
                    tick = ticks[asset.id],
                    quote = quotes[asset.id],
                )
            },
            isLoading = false,
            isEmpty = assets.isEmpty(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WatchlistUiState(isLoading = true),
    )

    init {
        watchlist
            .map { it.map(CryptoAsset::id) }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf<LivePrice>()
                else repository.observeLivePrices(ids)
            }
            .catch { Log.w(TAG, "live prices flow error", it) }
            .onEach { tick -> _liveTicks.update { it + (tick.id to tick) } }
            .launchIn(viewModelScope)

        watchlist
            .onEach { items ->
                val ids = items.map(CryptoAsset::id).toSet()
                _quoteCache.update { cache -> cache.filterKeys { it in ids } }
                items.filter { it.id !in _quoteCache.value }.forEach { fetchQuote(it.id) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (true) {
                delay(QUOTE_REFRESH_INTERVAL_MS)
                watchlist.value.forEach { fetchQuote(it.id) }
            }
        }
    }

    private fun fetchQuote(id: String) {
        viewModelScope.launch {
            repository.getAsset(id)
                .onSuccess { asset -> _quoteCache.update { it + (id to asset) } }
                .onFailure { Log.w(TAG, "getAsset($id) failed: ${it.message}") }
        }
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.LocalQueryChanged -> _localQuery.value = event.query
            is WatchlistEvent.RemoveAsset       -> removeAsset(event.id)
            is WatchlistEvent.AssetClicked      -> Unit
        }
    }

    private fun removeAsset(id: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(id)
            _quoteCache.update { it - id }
        }
    }
}
