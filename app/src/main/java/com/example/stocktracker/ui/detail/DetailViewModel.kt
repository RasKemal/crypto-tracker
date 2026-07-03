package com.example.stocktracker.ui.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.R
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.navigation.AppDestination
import com.example.stocktracker.ui.util.LoadState
import com.example.stocktracker.ui.util.mapLivePrice
import com.example.stocktracker.ui.util.toDetailStableUiModel
import com.example.stocktracker.ui.util.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DetailVM"

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CryptoRepository,
) : ViewModel() {

    private val assetId: String =
        checkNotNull(savedStateHandle[AppDestination.Detail.ARG_ID]) {
            "Detail route is missing required '${AppDestination.Detail.ARG_ID}' argument."
        }
    private val initialSymbol: String =
        savedStateHandle.get<String>(AppDestination.Detail.ARG_SYMBOL).orEmpty()
            .ifBlank { assetId }
    private val initialName: String =
        savedStateHandle.get<String>(AppDestination.Detail.ARG_NAME).orEmpty()
            .ifBlank { assetId }

    private val _asset = MutableStateFlow<CryptoAsset?>(null)
    private var liveTick: LivePrice? = null
    private val _content = MutableStateFlow<LoadState<CryptoAsset>>(LoadState.Loading)
    private val _livePrice = MutableStateFlow(PriceDisplayUiModel.Loading)

    val livePrice: StateFlow<PriceDisplayUiModel> = _livePrice

    private val watchlistIds: StateFlow<Set<String>> = repository.getWatchlist()
        .map { list -> list.map(CryptoAsset::id).toSet() }
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val uiState: StateFlow<DetailUiState> = combine(
        _asset, _content, watchlistIds,
    ) { asset, content, watchlist ->
        val mappedContent = when (content) {
            LoadState.Loading -> LoadState.Loading
            is LoadState.Error -> LoadState.Error(content.message)
            is LoadState.Success -> {
                val resolved = asset ?: content.data
                LoadState.Success(resolved.toDetailStableUiModel())
            }
        }
        DetailUiState(
            id = assetId,
            symbol = asset?.symbol?.ifBlank { initialSymbol } ?: initialSymbol,
            name = asset?.name?.ifBlank { initialName } ?: initialName,
            content = mappedContent,
            isInWatchlist = assetId in watchlist,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DetailUiState(
            id = assetId,
            symbol = initialSymbol,
            name = initialName,
            content = LoadState.Loading,
        ),
    )

    init {
        loadAsset()

        repository.observeLivePrices(listOf(assetId))
            .catch { Log.e(TAG, "live tick flow error", it) }
            .onEach { tick ->
                if (tick.id == assetId) {
                    liveTick = tick
                    syncLivePrice()
                }
            }
            .launchIn(viewModelScope)

    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.Refresh, DetailEvent.Retry -> loadAsset()
            DetailEvent.ToggleWatchlist -> toggleWatchlist()
        }
    }


    private fun loadAsset(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _asset.value == null) {
                _content.value = LoadState.Loading
            }
            repository.getAsset(assetId)
                .onSuccess { asset ->
                    _asset.value = asset
                    _content.value = LoadState.Success(asset)
                    syncLivePrice()
                }
                .onFailure { e ->
                    Log.e(TAG, "getAsset($assetId) failed", e)
                    if (_asset.value == null) {
                        _content.value = LoadState.Error(e.toUiMessage(R.string.error_asset_data))
                    }
                }
        }
    }

    private fun syncLivePrice() {
        val quote = _asset.value ?: return
        _livePrice.value = mapLivePrice(tick = liveTick, asset = quote)
    }

    private fun toggleWatchlist() {
        val currentAsset = _asset.value ?: CryptoAsset(
            id = assetId,
            symbol = initialSymbol,
            name = initialName,
            quoteAsset = "USDT",
            priceUsd = 0.0,
            changePercent24Hr = 0.0,
        )
        viewModelScope.launch {
            if (assetId in watchlistIds.value) repository.removeFromWatchlist(assetId)
            else repository.addToWatchlist(currentAsset)
        }
    }
}
