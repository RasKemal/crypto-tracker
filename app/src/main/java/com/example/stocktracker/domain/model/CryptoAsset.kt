package com.example.stocktracker.domain.model

data class CryptoAsset(
    val id: String,
    val symbol: String,
    val name: String,
    val quoteAsset: String,
    val priceUsd: Double,
    val changePercent24Hr: Double,
    val high24Hr: Double? = null,
    val low24Hr: Double? = null,
    val volumeUsd24Hr: Double? = null,
    val vwap24Hr: Double? = null,
)
