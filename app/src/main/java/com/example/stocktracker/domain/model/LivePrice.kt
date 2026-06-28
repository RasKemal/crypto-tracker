package com.example.stocktracker.domain.model

// Ephemeral — must never be persisted to Room. ViewModels merge this with
// watchlist data from Room via `combine`.
data class LivePrice(
    val symbol: String,
    val price: Double,
    val volume: Double,
    val timestamp: Long,
)
