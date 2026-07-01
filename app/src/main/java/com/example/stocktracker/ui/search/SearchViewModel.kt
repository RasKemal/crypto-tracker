package com.example.stocktracker.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.usecase.GetPopularCryptosUseCase
import com.example.stocktracker.domain.usecase.SearchAssetsUseCase
import com.example.stocktracker.ui.common.toUiMessage
import com.example.stocktracker.R
import com.example.stocktracker.ui.common.LoadState
import com.example.stocktracker.ui.common.toUiMessage
import com.example.stocktracker.core.helpers.mapLivePrice
import com.example.stocktracker.core.helpers.toAssetUiModel
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CryptoRepository,
    private val searchAssets: SearchAssetsUseCase,
    private val getPopularCryptos: GetPopularCryptosUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _isShowingPopular = MutableStateFlow(true)
    private val _content = MutableStateFlow<LoadState<List<CryptoAsset>>>(LoadState.Loading)
    private val _liveTicks = MutableStateFlow<Map<String, LivePrice>>(emptyMap())
    private val _livePrices = MutableStateFlow<Map<String, PriceDisplayUiModel>>(emptyMap())

    val livePrices: StateFlow<Map<String, PriceDisplayUiModel>> = _livePrices

    private val watchlistIds: StateFlow<Set<String>> = repository.getWatchlist()
        .map { list -> list.map(CryptoAsset::id).toSet() }
        .catch { Log.w(TAG, "watchlist flow error", it); emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val assets: List<CryptoAsset>
        get() = (_content.value as? LoadState.Success)?.data.orEmpty()

    val uiState: StateFlow<SearchUiState> = combine(
        _query, _isShowingPopular, _content, watchlistIds,
    ) { query, showingPopular, content, watchlist ->
        val mappedContent = when (content) {
            LoadState.Loading -> LoadState.Loading
            is LoadState.Error -> LoadState.Error(content.message)
            is LoadState.Success -> LoadState.Success(
                content.data.map { asset ->
                    asset.toAssetUiModel(
                        isInWatchlist = asset.id in watchlist,
                    )
                },
            )
        }
        SearchUiState(
            query = query,
            isShowingPopular = showingPopular,
            content = mappedContent,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SearchUiState(content = LoadState.Loading),
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

        _content
            .map { (it as? LoadState.Success)?.data.orEmpty().map(CryptoAsset::id) }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf<LivePrice>()
                else repository.observeLivePrices(ids)
            }
            .catch { Log.w(TAG, "live prices flow error", it) }
            .onEach { tick ->
                _liveTicks.update { it + (tick.id to tick) }
                syncLivePrice(tick.id)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> _query.update { event.query }
            is SearchEvent.WatchlistToggled -> toggleWatchlist(event.asset)
            SearchEvent.Retry -> if (_query.value.isBlank()) loadPopular() else performSearch(_query.value)
        }
    }

    private fun loadPopular() {
        viewModelScope.launch {
            _content.value = LoadState.Loading
            getPopularCryptos()
                .onSuccess { popular ->
                    Log.d(TAG, "loadPopular → ${popular.size} assets")
                    applySnapshot(popular, showingPopular = true)
                }
                .onFailure { e ->
                    Log.e(TAG, "loadPopular failed", e)
                    _livePrices.value = emptyMap()
                    _content.value = LoadState.Error(
                        e.toUiMessage(R.string.error_popular_assets),
                    )
                }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _content.value = LoadState.Loading
            searchAssets(query)
                .onSuccess { results ->
                    Log.d(TAG, "searchAssets(\"$query\") → ${results.size} results")
                    applySnapshot(results, showingPopular = false)
                }
                .onFailure { e ->
                    Log.e(TAG, "searchAssets(\"$query\") failed", e)
                    _livePrices.value = emptyMap()
                    _isShowingPopular.value = false
                    _content.value = LoadState.Error(
                        e.toUiMessage(R.string.error_search),
                    )
                }
        }
    }

    private fun applySnapshot(assets: List<CryptoAsset>, showingPopular: Boolean) {
        _isShowingPopular.value = showingPopular
        _content.value = LoadState.Success(assets)
        _livePrices.value = assets.associate { asset ->
            asset.id to mapLivePrice(
                tick = _liveTicks.value[asset.id],
                quote = asset,
            )
        }
    }

    private fun syncLivePrice(id: String) {
        val quote = assets.firstOrNull { it.id == id } ?: return
        val tick = _liveTicks.value[id]
        _livePrices.update { prices ->
            prices + (id to mapLivePrice(tick = tick, quote = quote))
        }
    }

    private fun toggleWatchlist(asset: AssetUiModel) {
        viewModelScope.launch {
            if (asset.id in watchlistIds.value) {
                repository.removeFromWatchlist(asset.id)
            } else {
                val cached = assets.firstOrNull { it.id == asset.id }
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
