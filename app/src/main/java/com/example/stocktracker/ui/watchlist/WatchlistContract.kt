package com.example.stocktracker.ui.watchlist

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.stocktracker.ui.model.CryptoAssetUiModel

@Immutable
data class WatchlistUiState(
    val isLoading: Boolean = true,
    val localQuery: String = "",
    val items: List<CryptoAssetUiModel> = emptyList(),
    val isEmpty: Boolean = false,
    @param:StringRes val bannerMessageRes: Int? = null,
)

sealed interface WatchlistEvent {
    data class LocalQueryChanged(val query: String) : WatchlistEvent
    data class RemoveAsset(val id: String) : WatchlistEvent
}
