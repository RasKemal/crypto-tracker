package com.example.stocktracker.domain.model

data class LivePrice(
    val id: String,
    val price: Double,
    val changePercent24Hr: Double? = null,
)
