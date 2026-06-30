package com.example.stocktracker.ui.model

import androidx.compose.runtime.Immutable
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import java.util.Locale
import kotlin.math.abs

@Immutable
data class AssetUiModel(
    val id: String,
    val symbol: String,
    val name: String,
    val rank: Int,
    val livePriceUsd: Double,
    val formattedPrice: String,
    val formattedChange: String,
    val isPositive: Boolean,
    val isInWatchlist: Boolean,
)

@Immutable
data class WatchlistItemUiModel(
    val id: String,
    val symbol: String,
    val name: String,
    val rank: Int,
    val livePriceUsd: Double,
    val formattedPrice: String,
    val formattedChange: String,
    val isPositive: Boolean,
)

fun CryptoAsset.toSearchUiModel(
    rank: Int,
    tick: LivePrice? = null,
    isInWatchlist: Boolean,
): AssetUiModel {
    val effectivePrice = tick?.price ?: priceUsd
    val effectiveChange = tick?.changePercent24Hr ?: changePercent24Hr
    return AssetUiModel(
        id = id,
        symbol = symbol,
        name = name,
        rank = rank,
        livePriceUsd = effectivePrice,
        formattedPrice = effectivePrice.formatUsd(),
        formattedChange = effectiveChange.formatChangePercent(),
        isPositive = effectiveChange >= 0.0,
        isInWatchlist = isInWatchlist,
    )
}

fun CryptoAsset.toWatchlistItemUiModel(
    rank: Int,
    tick: LivePrice? = null,
    quote: CryptoAsset? = null,
): WatchlistItemUiModel {
    val effectivePrice = tick?.price
        ?: quote?.priceUsd
        ?: priceUsd.takeIf { it > 0.0 }
    val effectiveChange = tick?.changePercent24Hr
        ?: quote?.changePercent24Hr
        ?: changePercent24Hr
    return WatchlistItemUiModel(
        id = id,
        symbol = quote?.symbol?.takeIf { it.isNotBlank() } ?: symbol,
        name = quote?.name?.takeIf { it.isNotBlank() } ?: name,
        rank = rank,
        livePriceUsd = effectivePrice ?: 0.0,
        formattedPrice = effectivePrice?.formatUsd() ?: "--",
        formattedChange = effectiveChange.formatChangePercent(),
        isPositive = effectiveChange >= 0.0,
    )
}

fun Double.formatUsd(): String = when {
    this == 0.0  -> "--"
    this >= 1.0  -> String.format(Locale.US, "$%,.2f", this)
    this >= 0.01 -> String.format(Locale.US, "$%.4f", this)
    else         -> String.format(Locale.US, "$%.6f", this)
}

fun Double.formatChangePercent(): String {
    val numStr = String.format(Locale("tr", "TR"), "%.2f", abs(this))
    return if (this >= 0.0) "%$numStr" else "-%$numStr"
}

fun Double.formatLargeUsd(): String = when {
    this >= 1e12 -> String.format(Locale.US, "$%,.2fT", this / 1e12)
    this >= 1e9  -> String.format(Locale.US, "$%,.2fB", this / 1e9)
    this >= 1e6  -> String.format(Locale.US, "$%,.2fM", this / 1e6)
    this >= 1e3  -> String.format(Locale.US, "$%,.2fK", this / 1e3)
    else         -> String.format(Locale.US, "$%,.2f", this)
}
