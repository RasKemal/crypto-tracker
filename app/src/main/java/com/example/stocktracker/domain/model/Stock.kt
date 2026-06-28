package com.example.stocktracker.domain.model

data class Stock(
    val symbol: String,
    val displaySymbol: String,
    val description: String,
    val type: String,
    val exchange: String,
    val isInWatchlist: Boolean = false,
)
