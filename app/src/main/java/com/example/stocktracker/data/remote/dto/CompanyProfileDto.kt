package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

// All fields nullable — Finnhub returns {} for symbols not on the free tier.
data class CompanyProfileDto(
    @SerializedName("country") val country: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("exchange") val exchange: String?,
    @SerializedName("ipo") val ipo: String?,
    @SerializedName("marketCapitalization") val marketCap: Double?,
    @SerializedName("name") val name: String?,
    @SerializedName("shareOutstanding") val sharesOutstanding: Double?,
    @SerializedName("ticker") val ticker: String?,
    @SerializedName("weburl") val webUrl: String?,
    @SerializedName("logo") val logoUrl: String?,
    @SerializedName("finnhubIndustry") val industry: String?,
)
