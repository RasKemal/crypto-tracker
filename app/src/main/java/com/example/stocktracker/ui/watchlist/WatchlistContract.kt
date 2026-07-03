package com.example.stocktracker.ui.watchlist

import androidx.compose.runtime.Immutable
import com.example.stocktracker.ui.model.CryptoAssetUiModel

@Immutable
data class WatchlistUiState(
    val isLoading: Boolean = true,
    val localQuery: String = "",
    val items: List<CryptoAssetUiModel> = emptyList(),
    val isEmpty: Boolean = false,
)

sealed interface WatchlistEvent {
    data class LocalQueryChanged(val query: String) : WatchlistEvent
    data class RemoveAsset(val id: String) : WatchlistEvent
}
