package com.example.stocktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stocktracker.data.local.dao.CryptoDao
import com.example.stocktracker.data.local.entity.CryptoEntity

@Database(
    entities = [CryptoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MidasDatabase : RoomDatabase() {
    abstract fun cryptoDao(): CryptoDao

    companion object {
        const val DATABASE_NAME = "midas_binance.db"
    }
}
