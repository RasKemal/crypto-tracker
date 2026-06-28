package com.example.stocktracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Only static metadata is stored. Live prices are intentionally excluded —
// they come from WebSocket and are merged in the ViewModel via combine.
@Entity(tableName = "watchlist")
data class StockEntity(
    @PrimaryKey
    val symbol: String,
    val displaySymbol: String,
    val description: String,
    val type: String,
    val exchange: String,
    val addedAt: Long = System.currentTimeMillis(),
)
