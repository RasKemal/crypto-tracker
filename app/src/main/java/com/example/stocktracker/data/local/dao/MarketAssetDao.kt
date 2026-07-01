package com.example.stocktracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stocktracker.data.local.entity.MarketAssetEntity

@Dao
interface MarketAssetDao {

    @Query("SELECT * FROM market_assets ORDER BY volumeUsd24Hr DESC")
    suspend fun getAll(): List<MarketAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<MarketAssetEntity>)

    @Query("DELETE FROM market_assets")
    suspend fun deleteAll()
}
