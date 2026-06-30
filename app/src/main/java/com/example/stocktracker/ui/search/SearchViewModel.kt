package com.example.stocktracker.ui.search

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.repository.PopularCryptoRepository
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.toSearchUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SearchVM"
private const val SEARCH_DEBOUNCE_MS = 350L

@Immutable
data class SearchUiState(
    val query: String = "",
    val isShowingPopular: Boolean = true,
    val assets: List<AssetUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class AssetClicked(val asset: AssetUiModel) : SearchEvent
    data class WatchlistToggled(val asset: AssetUiModel) : SearchEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CryptoRepository,
    private val popularRepository: PopularCryptoRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _assets = MutableStateFlow<List<CryptoAsset>>(emptyList())
    private val _isShowingPopular = MutableStateFlow(true)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _liveTicks = MutableStateFlow<Map<String, LivePrice>>(emptyMap())

    private val watchlistIds: StateFlow<Set<String>> = repository.getWatchlist()
        .map { list -> list.map(CryptoAsset::id).toSet() }
        .catch { Log.w(TAG, "watchlist flow error", it); emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private data class CoreSnapshot(
        val query: String,
        val assets: List<CryptoAsset>,
        val showingPopular: Boolean,
        val loading: Boolean,
        val error: String?,
    )

    private val core: StateFlow<CoreSnapshot> = combine(
        _query, _assets, _isShowingPopular, _isLoading, _error,
    ) { q, a, p, l, e -> CoreSnapshot(q, a, p, l, e) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            CoreSnapshot("", emptyList(), true, true, null),
        )

    val uiState: StateFlow<SearchUiState> = combine(
        core, _liveTicks, watchlistIds,
    ) { snap, ticks, watchlist ->
        SearchUiState(
            query = snap.query,
            isShowingPopular = snap.showingPopular,
            assets = snap.assets.mapIndexed { index, asset ->
                asset.toSearchUiModel(
                    rank = index + 1,
                    tick = ticks[asset.id],
                    isInWatchlist = asset.id in watchlist,
                )
            },
            isLoading = snap.loading,
            error = snap.error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SearchUiState(isLoading = true),
    )

    init {
        loadPopular()

        _query
            .drop(1)
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) loadPopular()
                else performSearch(query)
            }
            .launchIn(viewModelScope)

        _assets
            .map { it.map(CryptoAsset::id) }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf<LivePrice>()
                else repository.observeLivePrices(ids)
            }
            .catch { Log.w(TAG, "live prices flow error", it) }
            .onEach { tick -> _liveTicks.update { it + (tick.id to tick) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged     -> _query.update { event.query }
            is SearchEvent.WatchlistToggled -> toggleWatchlist(event.asset)
            is SearchEvent.AssetClicked     -> Unit
        }
    }

    private fun loadPopular() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            popularRepository.getPopularCryptos()
                .onSuccess { popular ->
                    Log.d(TAG, "loadPopular → ${popular.size} assets")
                    _assets.value = popular
                    _isShowingPopular.value = true
                }
                .onFailure { e ->
                    Log.e(TAG, "loadPopular failed", e)
                    _error.value = e.message ?: "Popüler varlıklar yüklenemedi"
                    _assets.value = emptyList()
                }
            _isLoading.value = false
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.searchAssets(query)
                .onSuccess { results ->
                    Log.d(TAG, "searchAssets(\"$query\") → ${results.size} results")
                    _assets.value = results
                    _isShowingPopular.value = false
                }
                .onFailure { e ->
                    Log.e(TAG, "searchAssets(\"$query\") failed", e)
                    _error.value = e.message ?: "Arama başarısız"
                    _assets.value = emptyList()
                    _isShowingPopular.value = false
                }
            _isLoading.value = false
        }
    }

    private fun toggleWatchlist(asset: AssetUiModel) {
        viewModelScope.launch {
            if (asset.id in watchlistIds.value) {
                repository.removeFromWatchlist(asset.id)
            } else {
                val cached = _assets.value.firstOrNull { it.id == asset.id }
                    ?: CryptoAsset(
                        id = asset.id,
                        symbol = asset.symbol,
                        name = asset.name,
                        quoteAsset = "USDT",
                        priceUsd = 0.0,
                        changePercent24Hr = 0.0,
                    )
                repository.addToWatchlist(cached)
            }
        }
    }
}
