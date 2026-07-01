package com.example.stocktracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_assets")
data class MarketAssetEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val priceUsd: Double,
    val changePercent24Hr: Double,
    val volumeUsd24Hr: Double?,
)
