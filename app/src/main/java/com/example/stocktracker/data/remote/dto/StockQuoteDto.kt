package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

// Field names follow Finnhub's single-letter abbreviations.
data class StockQuoteDto(
    @SerializedName("c") val currentPrice: Double,
    @SerializedName("d") val change: Double,
    @SerializedName("dp") val changePercent: Double,
    @SerializedName("h") val highPrice: Double,
    @SerializedName("l") val lowPrice: Double,
    @SerializedName("o") val openPrice: Double,
    @SerializedName("pc") val previousClose: Double,
    @SerializedName("t") val timestamp: Long,
)
