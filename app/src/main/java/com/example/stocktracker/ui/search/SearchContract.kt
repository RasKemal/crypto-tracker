package com.example.stocktracker.ui.search

import androidx.compose.runtime.Immutable
import com.example.stocktracker.ui.common.LoadState
import com.example.stocktracker.ui.model.AssetUiModel

@Immutable
data class SearchUiState(
    val query: String = "",
    val isShowingPopular: Boolean = true,
    val content: LoadState<List<AssetUiModel>> = LoadState.Loading,
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class WatchlistToggled(val asset: AssetUiModel) : SearchEvent
    data object Retry : SearchEvent
}
