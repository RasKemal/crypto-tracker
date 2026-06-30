package com.example.stocktracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.example.stocktracker.ui.model.PriceDisplayUiModel

@Composable
fun LivePriceColumn(
    assetId: String,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    modifier: Modifier = Modifier,
) {
    PriceColumn(
        price = livePrices.value[assetId] ?: PriceDisplayUiModel.Loading,
        modifier = modifier,
    )
}
