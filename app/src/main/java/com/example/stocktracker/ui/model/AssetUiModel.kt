package com.example.stocktracker.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class AssetUiModel(
    val id: String,
    val symbol: String,
    val name: String,
    val rank: Int,
    val isInWatchlist: Boolean = false,
)

@Immutable
data class DetailStableUiModel(
    val pairLabel: String,
    val rangeStats: List<StatRowUiModel>,
    val activityStats: List<StatRowUiModel>,
)

@Immutable
data class StatRowUiModel(
    val label: String,
    val value: String,
)
