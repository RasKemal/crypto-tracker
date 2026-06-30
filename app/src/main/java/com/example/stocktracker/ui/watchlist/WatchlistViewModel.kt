package com.example.stocktracker.ui.watchlist

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.util.userMessage
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.model.WatchlistStableItemUiModel
import com.example.stocktracker.ui.model.mapWatchlistLivePrice
import com.example.stocktracker.ui.model.toWatchlistStableItemUiModel
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

private data class QuoteFetchState(
    val quote: CryptoAsset? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Immutable
data class WatchlistUiState(
    val localQuery: String = "",
    val items: List<WatchlistStableItemUiModel> = emptyList(),
    val isEmpty: Boolean = false,
    val bannerMessage: String? = null,
)

sealed interface WatchlistEvent {
    data class LocalQueryChanged(val query: String) : WatchlistEvent
    data class RemoveAsset(val id: String) : WatchlistEvent
    data class AssetClicked(val item: WatchlistStableItemUiModel) : WatchlistEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: CryptoRepository,
) : ViewModel() {

    private val _localQuery = MutableStateFlow("")
    private val _liveTicks = MutableStateFlow<Map<String, LivePrice>>(emptyMap())
    private val _quoteStates = MutableStateFlow<Map<String, QuoteFetchState>>(emptyMap())
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private val _livePrices = MutableStateFlow<Map<String, PriceDisplayUiModel>>(emptyMap())

    val livePrices: StateFlow<Map<String, PriceDisplayUiModel>> = _livePrices

    private val watchlist: StateFlow<List<CryptoAsset>> = repository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlist, _localQuery, _bannerMessage,
    ) { assets, query, banner ->
        val filtered = if (query.isBlank()) assets
        else assets.filter {
            it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
        WatchlistUiState(
            localQuery = query,
            items = filtered.mapIndexed { index, asset ->
                asset.toWatchlistStableItemUiModel(rank = index + 1)
            },
            isEmpty = assets.isEmpty(),
            bannerMessage = banner,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WatchlistUiState(),
    )

    init {
        watchlist
            .map { it.map(CryptoAsset::id) }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf<LivePrice>()
                else repository.observeLivePrices(ids)
            }
            .catch { e ->
                Log.w(TAG, "live prices flow error", e)
                _bannerMessage.value = "Canlı fiyat bağlantısı kesildi"
            }
            .onEach {
                _bannerMessage.value = null
                _liveTicks.update { map -> map + (it.id to it) }
                syncLivePrice(it.id)
            }
            .launchIn(viewModelScope)

        watchlist
            .onEach { items ->
                val ids = items.map(CryptoAsset::id).toSet()
                _quoteStates.update { states -> states.filterKeys { it in ids } }
                _livePrices.update { prices -> prices.filterKeys { it in ids } }
                items.forEach { asset ->
                    if (asset.id !in _quoteStates.value) fetchQuote(asset.id)
                    else syncLivePrice(asset.id)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (true) {
                delay(QUOTE_REFRESH_INTERVAL_MS)
                watchlist.value.forEach { fetchQuote(it.id, isRefresh = true) }
            }
        }
    }

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.LocalQueryChanged -> _localQuery.value = event.query
            is WatchlistEvent.RemoveAsset -> removeAsset(event.id)
            is WatchlistEvent.AssetClicked -> Unit
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
                    val message = e.userMessage("Fiyat yüklenemedi")
                    _quoteStates.update { states ->
                        val previous = states[id]
                        states + (id to QuoteFetchState(
                            quote = previous?.quote,
                            error = if (previous?.quote == null) message else null,
                        ))
                    }
                    syncLivePrice(id)
                    if (isRefresh) {
                        _bannerMessage.value = "Bazı fiyatlar güncellenemedi"
                    }
                }
        }
    }

    private fun syncLivePrice(id: String) {
        val asset = watchlist.value.firstOrNull { it.id == id } ?: return
        val quoteState = _quoteStates.value[id]
        val quoteFailed = quoteState?.error != null && quoteState.quote == null
        val quoteRequested = quoteState != null
        val quote = quoteState?.quote ?: asset.takeIf { quoteRequested && !quoteFailed }
        _livePrices.update { prices ->
            prices + (id to mapWatchlistLivePrice(
                tick = _liveTicks.value[id],
                quote = quote,
                quoteFailed = quoteFailed,
                quoteRequested = quoteRequested,
            ))
        }
    }

    private fun removeAsset(id: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(id)
            _quoteStates.update { it - id }
            _livePrices.update { it - id }
        }
    }
}
