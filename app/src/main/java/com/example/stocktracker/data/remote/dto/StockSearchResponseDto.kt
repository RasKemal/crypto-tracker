package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StockSearchResponseDto(
    @SerializedName("count") val count: Int,
    @SerializedName("result") val result: List<StockSearchResultDto>,
)

data class StockSearchResultDto(
    @SerializedName("description") val description: String,
    @SerializedName("displaySymbol") val displaySymbol: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("type") val type: String,
    // Always null on the free tier — classification uses symbol patterns instead.
    @SerializedName("primaryExchange") val primaryExchange: String? = null,
)
