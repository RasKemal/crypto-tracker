package com.example.stocktracker.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class PriceDisplayUiModel(
    val formattedPrice: String,
    val formattedChange: String,
    val isPositive: Boolean,
    val priceUsd: Double? = null,
    val isLoading: Boolean = false,
    val priceLoadFailed: Boolean = false,
) {
    companion object {
        val Loading = PriceDisplayUiModel(
            formattedPrice = "",
            formattedChange = "",
            isPositive = true,
            isLoading = true,
        )
    }
}
