package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

// type = "trade" | "ping" | "error"
data class WebSocketMessageDto(
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: List<TradeDto>?,
)

// Short field names are Finnhub's own convention (s, p, t, v).
data class TradeDto(
    @SerializedName("s") val symbol: String,
    @SerializedName("p") val price: Double,
    @SerializedName("t") val timestamp: Long,
    @SerializedName("v") val volume: Double,
)
