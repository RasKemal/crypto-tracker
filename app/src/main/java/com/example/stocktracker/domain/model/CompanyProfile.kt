package com.example.stocktracker.domain.model

// Returns empty strings / null for crypto or symbols not on the free tier.
data class CompanyProfile(
    val symbol: String,
    val name: String,
    val country: String,
    val currency: String,
    val exchange: String,
    val ipo: String,
    val marketCap: Double?,
    val sharesOutstanding: Double?,
    val webUrl: String,
    val logoUrl: String,
    val industry: String,
)
