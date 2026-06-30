package com.example.stocktracker.ui.model

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice

fun CryptoAsset.toAssetUiModel(rank: Int, isInWatchlist: Boolean = false): AssetUiModel =
    AssetUiModel(
        id = id,
        symbol = symbol,
        name = name,
        rank = rank,
        isInWatchlist = isInWatchlist,
    )

fun CryptoAsset.toDetailStableUiModel(): DetailStableUiModel = DetailStableUiModel(
    pairLabel = "$symbol/$quoteAsset",
    rangeStats = listOf(
        StatRowUiModel("En Yüksek", high24Hr.formatUsdOrDash()),
        StatRowUiModel("En Düşük", low24Hr.formatUsdOrDash()),
        StatRowUiModel("Ortalama (VWAP)", vwap24Hr.formatUsdOrDash()),
    ),
    activityStats = listOf(
        StatRowUiModel("24s Hacim", volumeUsd24Hr.formatLargeUsdOrDash()),
        StatRowUiModel("Çift", "$symbol/$quoteAsset"),
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
            errorMessage = "Fiyat yüklenemedi",
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
