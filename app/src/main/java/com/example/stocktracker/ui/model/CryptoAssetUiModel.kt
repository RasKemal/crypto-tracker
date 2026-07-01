package com.example.stocktracker.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class CryptoAssetUiModel(
    val id: String,
    val symbol: String,
    val name: String,
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
    @param:StringRes val labelRes: Int,
    val value: String,
)
