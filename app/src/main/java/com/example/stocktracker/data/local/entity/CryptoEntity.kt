package com.example.stocktracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class CryptoEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
)
