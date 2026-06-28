package com.example.stocktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stocktracker.data.local.dao.StockDao
import com.example.stocktracker.data.local.entity.StockEntity

@Database(
    entities = [StockEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MidasDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        const val DATABASE_NAME = "midas_database"
    }
}
