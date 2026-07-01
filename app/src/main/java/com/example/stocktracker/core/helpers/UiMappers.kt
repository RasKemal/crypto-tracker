package com.example.stocktracker.core.helpers

import com.example.stocktracker.R
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.DetailStableUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.model.StatRowUiModel

fun CryptoAsset.toAssetUiModel(isInWatchlist: Boolean = false): AssetUiModel =
    AssetUiModel(
        id = id,
        symbol = symbol,
        name = name,
        isInWatchlist = isInWatchlist,
    )

fun CryptoAsset.toDetailStableUiModel(): DetailStableUiModel =
    DetailStableUiModel(
        pairLabel = "$symbol/$quoteAsset",
        rangeStats = listOf(
            StatRowUiModel(R.string.stat_high, high24Hr.formatUsdOrDash()),
            StatRowUiModel(R.string.stat_low, low24Hr.formatUsdOrDash()),
            StatRowUiModel(R.string.stat_vwap, vwap24Hr.formatUsdOrDash()),
        ),
        activityStats = listOf(
            StatRowUiModel(R.string.stat_volume_24h, volumeUsd24Hr.formatLargeUsdOrDash()),
            StatRowUiModel(R.string.stat_pair, "$symbol/$quoteAsset"),
        ),
    )

fun mapLivePrice(
    tick: LivePrice?,
    quote: CryptoAsset?,
    quoteFailed: Boolean = false,
    isLoading: Boolean = false,
): PriceDisplayUiModel {
    if (tick != null) {
        val change = tick.changePercent24Hr ?: quote?.changePercent24Hr ?: 0.0
        return PriceDisplayUiModel(
            formattedPrice = tick.price.formatUsd(),
            formattedChange = change.formatChangePercent(),
            isPositive = change >= 0.0,
            priceUsd = tick.price,
        )
    }
    if (quote != null) {
        return PriceDisplayUiModel(
            formattedPrice = quote.priceUsd.formatUsd(),
            formattedChange = quote.changePercent24Hr.formatChangePercent(),
            isPositive = quote.changePercent24Hr >= 0.0,
            priceUsd = quote.priceUsd,
        )
    }
    if (quoteFailed) {
        return PriceDisplayUiModel(
            formattedPrice = "--",
            formattedChange = "",
            isPositive = false,
            priceLoadFailed = true,
        )
    }
    if (isLoading) {
        return PriceDisplayUiModel.Loading
    }
    return PriceDisplayUiModel(
        formattedPrice = "--",
        formattedChange = "",
        isPositive = false,
    )
}
