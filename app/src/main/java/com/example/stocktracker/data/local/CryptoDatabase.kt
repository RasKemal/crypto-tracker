package com.example.stocktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stocktracker.data.local.dao.WatchlistDao
import com.example.stocktracker.data.local.dao.MarketAssetDao
import com.example.stocktracker.data.local.entity.WatchlistEntity
import com.example.stocktracker.data.local.entity.MarketAssetEntity

@Database(
    entities = [WatchlistEntity::class, MarketAssetEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun marketAssetDao(): MarketAssetDao

    companion object {
        const val DATABASE_NAME = "crypto_binance.db"
    }
}
