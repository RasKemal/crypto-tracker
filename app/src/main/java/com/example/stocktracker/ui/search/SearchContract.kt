package com.example.stocktracker.ui.search

import androidx.compose.runtime.Immutable
import com.example.stocktracker.ui.util.LoadState
import com.example.stocktracker.ui.model.CryptoAssetUiModel

@Immutable
data class SearchUiState(
    val query: String = "",
    val isShowingPopular: Boolean = true,
    val content: LoadState<List<CryptoAssetUiModel>> = LoadState.Loading,
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class WatchlistToggled(val asset: CryptoAssetUiModel) : SearchEvent
    data object Retry : SearchEvent
}
