package com.example.stocktracker.data.mapper

import com.example.stocktracker.data.local.entity.CryptoEntity
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.util.stripUsdStableQuote

internal fun Ticker24hDto.toDomain(): CryptoAsset {
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

fun CryptoEntity.toDomain(): CryptoAsset = CryptoAsset(
    id = id,
    symbol = symbol,
    name = name,
    quoteAsset = stripUsdStableQuote(id).let { id.removePrefix(it) },
    priceUsd = 0.0,
    changePercent24Hr = 0.0,
)

fun CryptoAsset.toEntity(): CryptoEntity = CryptoEntity(
    id = id,
    symbol = symbol,
    name = name,
)
