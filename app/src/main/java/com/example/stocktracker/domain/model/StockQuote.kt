package com.example.stocktracker.domain.model

// REST snapshot; used for detail screens and as the price baseline before the
// first WebSocket tick arrives. `currentPrice == 0.0` means no free-tier coverage.
data class StockQuote(
    val symbol: String,
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val openPrice: Double,
    val previousClose: Double,
)
