package com.example.stocktracker.ui.detail

import androidx.compose.runtime.Immutable
import com.example.stocktracker.ui.util.LoadState
import com.example.stocktracker.ui.model.DetailStableUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel

@Immutable
data class DetailUiState(
    val id: String,
    val symbol: String,
    val name: String,
    val content: LoadState<DetailStableUiModel>,
    val isInWatchlist: Boolean = false,
)

sealed interface DetailEvent {
    data object Refresh : DetailEvent
    data object ToggleWatchlist : DetailEvent
    data object Retry : DetailEvent
}
