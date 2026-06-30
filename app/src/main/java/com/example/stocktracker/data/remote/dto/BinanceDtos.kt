package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Ticker24hDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("priceChange") val priceChange: String? = null,
    @SerializedName("priceChangePercent") val priceChangePercent: String? = null,
    @SerializedName("weightedAvgPrice") val weightedAvgPrice: String? = null,
    @SerializedName("lastPrice") val lastPrice: String? = null,
    @SerializedName("openPrice") val openPrice: String? = null,
    @SerializedName("highPrice") val highPrice: String? = null,
    @SerializedName("lowPrice") val lowPrice: String? = null,
    @SerializedName("volume") val volume: String? = null,
    @SerializedName("quoteVolume") val quoteVolume: String? = null,
    @SerializedName("bidPrice") val bidPrice: String? = null,
    @SerializedName("askPrice") val askPrice: String? = null,
)

data class ExchangeInfoDto(
    @SerializedName("symbols") val symbols: List<SymbolInfoDto>,
)

data class SymbolInfoDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("status") val status: String,
    @SerializedName("baseAsset") val baseAsset: String,
    @SerializedName("quoteAsset") val quoteAsset: String,
)

data class CombinedStreamEnvelopeDto(
    @SerializedName("stream") val stream: String?,
    @SerializedName("data") val data: TickerStreamPayloadDto?,
)

data class TickerStreamPayloadDto(
    @SerializedName("e") val eventType: String? = null,
    @SerializedName("s") val symbol: String? = null,
    @SerializedName("c") val lastPrice: String? = null,
    @SerializedName("P") val priceChangePercent: String? = null,
    @SerializedName("w") val weightedAvgPrice: String? = null,
    @SerializedName("h") val highPrice: String? = null,
    @SerializedName("l") val lowPrice: String? = null,
    @SerializedName("q") val quoteVolume: String? = null,
)
