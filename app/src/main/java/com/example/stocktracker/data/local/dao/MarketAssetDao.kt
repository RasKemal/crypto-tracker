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

    @Query("SELECT * FROM market_assets WHERE volumeUsd24Hr > 0 ORDER BY volumeUsd24Hr DESC LIMIT :limit")
    suspend fun getPopularAssets(limit: Int = 10): List<MarketAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<MarketAssetEntity>)

    @Query("DELETE FROM market_assets")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM market_assets 
        WHERE symbol LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN symbol LIKE :query THEN 4
                WHEN symbol LIKE :query || '%' THEN 3
                WHEN name LIKE '%' || :query || '%' THEN 2
                WHEN symbol LIKE '%' || :query || '%' THEN 1
                ELSE 0 
            END DESC,
            volumeUsd24Hr DESC
        LIMIT :limit
    """)
    suspend fun searchAssets(query: String, limit: Int = 30): List<MarketAssetEntity>
}
