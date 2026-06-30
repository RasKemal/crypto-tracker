package com.example.stocktracker.domain.repository

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.MarketSnapshot
import kotlinx.coroutines.flow.Flow

interface CryptoRepository {

    suspend fun getMarketSnapshot(): Result<MarketSnapshot>

    suspend fun getAsset(id: String): Result<CryptoAsset>

    fun getWatchlist(): Flow<List<CryptoAsset>>

    fun observeLivePrices(ids: List<String>): Flow<LivePrice>

    suspend fun addToWatchlist(asset: CryptoAsset)

    suspend fun removeFromWatchlist(id: String)
}
