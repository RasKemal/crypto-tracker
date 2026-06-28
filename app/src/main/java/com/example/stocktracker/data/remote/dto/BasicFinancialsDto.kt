package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BasicFinancialsDto(
    @SerializedName("metric") val metric: MetricDto?,
    @SerializedName("symbol") val symbol: String?,
)

data class MetricDto(
    @SerializedName("52WeekHigh") val high52Week: Double?,
    @SerializedName("52WeekLow") val low52Week: Double?,
    @SerializedName("peBasicExclExtraTTM") val peRatio: Double?,
    @SerializedName("beta") val beta: Double?,
    @SerializedName("dividendYieldIndicatedAnnual") val dividendYield: Double?,
    @SerializedName("10DayAverageTradingVolume") val avgVolume10Day: Double?,
)
