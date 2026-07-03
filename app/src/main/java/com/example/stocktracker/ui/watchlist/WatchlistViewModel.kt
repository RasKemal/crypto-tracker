package com.example.stocktracker.ui.watchlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.util.mapLivePrice
import com.example.stocktracker.ui.util.toCryptoAssetUiModel
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
    private val _livePrices = MutableStateFlow<Map<String, PriceDisplayUiModel>>(emptyMap())
    private val liveTicks = mutableMapOf<String, LivePrice>()

    val livePrices: StateFlow<Map<String, PriceDisplayUiModel>> = _livePrices

    private val watchlist: StateFlow<List<CryptoAsset>?> = repository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlist, _localQuery,
    ) { assets, query ->
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
            items = filtered.map { asset -> asset.toCryptoAssetUiModel() },
            isEmpty = assets.isEmpty(),
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
            .catch { Log.e(TAG, "live tick flow error", it) }
            .onEach {
                liveTicks[it.id] = it
                syncLivePrice(it.id)
            }
            .launchIn(viewModelScope)

        watchlist
            .onEach { items ->
                if (items == null) return@onEach
                val ids = items.map(CryptoAsset::id).toSet()
                _livePrices.update { it.filterKeys { key -> key in ids } }
                liveTicks.keys.retainAll(ids)
                items.forEach { asset -> syncLivePrice(asset.id) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.LocalQueryChanged -> _localQuery.value = event.query
            is WatchlistEvent.RemoveAsset -> removeAsset(event.id)
        }
    }

    private fun syncLivePrice(id: String) {
        val asset = watchlist.value.orEmpty().firstOrNull { it.id == id } ?: return
        val tick = liveTicks[id]
        val cryptoAsset = asset.takeIf { it.priceUsd != 0.0 }
        _livePrices.update { prices ->
            prices + (id to mapLivePrice(
                tick = tick,
                asset = cryptoAsset,
                isLoading = tick == null && cryptoAsset == null,
            ))
        }
    }

    private fun removeAsset(id: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(id)
            _livePrices.update { it - id }
            liveTicks.remove(id)
        }
    }
}
