package com.example.stocktracker.domain.model

// All fields nullable — free-tier metric coverage varies by exchange.
data class BasicFinancials(
    val symbol: String,
    val high52Week: Double?,
    val low52Week: Double?,
    val peRatio: Double?,
    val beta: Double?,
    val dividendYield: Double?,
    val avgVolume10Day: Double?,
)
