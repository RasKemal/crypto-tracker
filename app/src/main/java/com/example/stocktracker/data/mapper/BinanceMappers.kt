package com.example.stocktracker.data.mapper

import com.example.stocktracker.data.local.entity.WatchlistEntity
import com.example.stocktracker.data.local.entity.MarketAssetEntity
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import com.example.stocktracker.domain.model.CryptoAsset

fun Ticker24hDto.toDomain(): CryptoAsset {
    val base = stripUsdStableQuote(symbol)
    val quote = symbol.removePrefix(base)
    return CryptoAsset(
        id = symbol,
        symbol = base,
        name = AssetNames.friendly(base),
        quoteAsset = quote,
        priceUsd = lastPrice?.toDoubleOrNull() ?: 0.0,
        changePercent24Hr = priceChangePercent?.toDoubleOrNull() ?: 0.0,
        high24Hr = highPrice?.toDoubleOrNull(),
        low24Hr = lowPrice?.toDoubleOrNull(),
        volumeUsd24Hr = quoteVolume?.toDoubleOrNull(),
        vwap24Hr = weightedAvgPrice?.toDoubleOrNull(),
    )
}

fun Ticker24hDto.toMarketEntity(): MarketAssetEntity? {
    if (symbol.length <= 3) return null
    val base = stripUsdStableQuote(symbol)
    val quote = symbol.removePrefix(base)
    if (!quote.isUsdStableQuote()) return null
    return MarketAssetEntity(
        id = symbol,
        symbol = base,
        name = AssetNames.friendly(base),
        priceUsd = lastPrice?.toDoubleOrNull() ?: 0.0,
        changePercent24Hr = priceChangePercent?.toDoubleOrNull() ?: 0.0,
        volumeUsd24Hr = quoteVolume?.toDoubleOrNull(),
    )
}

fun MarketAssetEntity.toDomain(): CryptoAsset = CryptoAsset(
    id = id,
    symbol = symbol,
    name = name,
    quoteAsset = stripUsdStableQuote(id).let { id.removePrefix(it) },
    priceUsd = priceUsd,
    changePercent24Hr = changePercent24Hr,
    volumeUsd24Hr = volumeUsd24Hr,
)

fun WatchlistEntity.toDomain(): CryptoAsset = CryptoAsset(
    id = id,
    symbol = symbol,
    name = name,
    quoteAsset = stripUsdStableQuote(id).let { id.removePrefix(it) },
    priceUsd = 0.0,
    changePercent24Hr = 0.0,
)

fun CryptoAsset.toWatchlistEntity(): WatchlistEntity = WatchlistEntity(
    id = id,
    symbol = symbol,
    name = name,
)

val USD_STABLE_QUOTES: List<String> = listOf("USDT", "USDC", "FDUSD")

fun String.isUsdStableQuote(): Boolean = this in USD_STABLE_QUOTES

fun stripUsdStableQuote(symbol: String): String =
    USD_STABLE_QUOTES
        .firstOrNull { symbol.endsWith(it) }
        ?.let { symbol.dropLast(it.length) }
        ?: symbol
