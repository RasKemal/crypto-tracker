package com.example.stocktracker.ui.detail

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DetailVM"

private const val SNAPSHOT_REFRESH_INTERVAL_MS = 60_000L

@Immutable
data class DetailUiState(
    val id: String,
    val symbol: String,
    val name: String,
    val asset: CryptoAsset? = null,
    val liveTick: LivePrice? = null,
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface DetailEvent {
    data object Refresh : DetailEvent
    data object ToggleWatchlist : DetailEvent
}

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
    private val _liveTick = MutableStateFlow<LivePrice?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    private val watchlistIds: StateFlow<Set<String>> = repository.getWatchlist()
        .map { list -> list.map(CryptoAsset::id).toSet() }
        .catch { Log.w(TAG, "watchlist flow error", it); emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val uiState: StateFlow<DetailUiState> = combine(
        _asset, _liveTick, watchlistIds, _isLoading, _error,
    ) { asset, tick, watchlist, loading, error ->
        DetailUiState(
            id = assetId,
            symbol = asset?.symbol?.ifBlank { initialSymbol } ?: initialSymbol,
            name   = asset?.name?.ifBlank   { initialName   } ?: initialName,
            asset = asset,
            liveTick = tick,
            isInWatchlist = assetId in watchlist,
            isLoading = loading,
            error = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DetailUiState(id = assetId, symbol = initialSymbol, name = initialName, isLoading = true),
    )

    init {
        loadAsset()

        repository.observeLivePrices(listOf(assetId))
            .catch { Log.w(TAG, "live tick flow error", it) }
            .onEach { tick -> if (tick.id == assetId) _liveTick.value = tick }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (true) {
                delay(SNAPSHOT_REFRESH_INTERVAL_MS)
                loadAsset()
            }
        }
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.Refresh         -> loadAsset()
            DetailEvent.ToggleWatchlist -> toggleWatchlist()
        }
    }

    private fun loadAsset() {
        viewModelScope.launch {
            _isLoading.update { _asset.value == null }
            _error.value = null
            repository.getAsset(assetId)
                .onSuccess { _asset.value = it }
                .onFailure { e ->
                    Log.e(TAG, "getAsset($assetId) failed", e)
                    if (_asset.value == null) _error.value = e.message ?: "Veri yüklenemedi"
                }
            _isLoading.value = false
        }
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
