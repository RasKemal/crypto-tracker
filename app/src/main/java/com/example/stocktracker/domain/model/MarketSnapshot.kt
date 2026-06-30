package com.example.stocktracker.domain.model

data class MarketSnapshot(
    val assetsBySymbol: Map<String, CryptoAsset>,
    val pairsByBaseAsset: Map<String, List<CryptoPair>>,
)

data class CryptoPair(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
)
