package com.example.stocktracker.ui.model

import androidx.compose.runtime.Immutable
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote
import java.util.Locale
import kotlin.math.abs

@Immutable
data class StockUiModel(
    val symbol: String,
    val displaySymbol: String,
    val description: String,
    val rank: Int,
    val formattedPrice: String,
    val formattedChange: String,
    val isPositive: Boolean,
    val isInWatchlist: Boolean,
)

@Immutable
data class WatchlistItemUiModel(
    val symbol: String,
    val displaySymbol: String,
    val description: String,
    val rank: Int,
    val formattedPrice: String,
    val formattedChange: String,
    val isPositive: Boolean,
)

// quote is null until the /quote response arrives; currentPrice == 0.0 means
// Finnhub has no free-tier coverage for this symbol.
fun Stock.toSearchUiModel(rank: Int, quote: StockQuote? = null): StockUiModel {
    val hasValidQuote = quote != null && quote.currentPrice != 0.0
    val changePercent = if (hasValidQuote) quote.changePercent else null
    return StockUiModel(
        symbol = symbol,
        displaySymbol = displaySymbol,
        description = description,
        rank = rank,
        formattedPrice = if (hasValidQuote) quote.currentPrice.formatPrice() else "--",
        formattedChange = changePercent?.formatChangePercent() ?: "--",
        isPositive = (changePercent ?: 0.0) >= 0.0,
        isInWatchlist = isInWatchlist,
    )
}

fun Stock.toWatchlistItemUiModel(
    rank: Int,
    livePrice: LivePrice?,
    quote: StockQuote?,
): WatchlistItemUiModel {
    val currentPrice: Double? = livePrice?.price
        ?: quote?.currentPrice?.takeIf { it != 0.0 }
    val changePercent: Double? = when {
        livePrice != null && quote != null && quote.previousClose != 0.0 ->
            (livePrice.price - quote.previousClose) / quote.previousClose * 100.0
        else -> quote?.changePercent?.takeIf { quote.currentPrice != 0.0 }
    }
    return WatchlistItemUiModel(
        symbol = symbol,
        displaySymbol = displaySymbol,
        description = description,
        rank = rank,
        formattedPrice = currentPrice?.formatPrice() ?: "--",
        formattedChange = changePercent?.formatChangePercent() ?: "--",
        isPositive = (changePercent ?: 0.0) >= 0.0,
    )
}

// Turkish style: +18.0 → "%18,00", -5.97 → "-%5,97"
fun Double.formatChangePercent(): String {
    val numStr = String.format(Locale("tr", "TR"), "%.2f", abs(this))
    return if (this >= 0.0) "%$numStr" else "-%$numStr"
}

fun Double.formatPrice(): String = String.format(Locale.US, "$%.2f", this)
