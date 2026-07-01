package com.example.stocktracker.data.repository

import android.util.Log
import com.example.stocktracker.data.cache.MarketSnapshotCache
import com.example.stocktracker.data.local.dao.CryptoDao
import com.example.stocktracker.data.mapper.toDomain
import com.example.stocktracker.data.mapper.toEntity
import com.example.stocktracker.data.remote.api.BinanceApi
import com.example.stocktracker.data.remote.websocket.BinanceStreamClient
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.MarketSnapshot
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.util.isUsdStableQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BinanceCryptoRepo"

@Singleton
class BinanceCryptoRepositoryImpl @Inject constructor(
    private val api: BinanceApi,
    private val streamClient: BinanceStreamClient,
    private val cache: MarketSnapshotCache,
    private val dao: CryptoDao,
) : CryptoRepository {

    override suspend fun getMarketSnapshot(): Result<MarketSnapshot> = runCatching {
        val tickers = cache.allTickers().getOrThrow()
        val assets = tickers
            .filter { it.symbol.length > 3 }
            .map { it.toDomain() }
            .filter { it.quoteAsset.isUsdStableQuote() }
            .sortedByDescending { it.volumeUsd24Hr ?: 0.0 }
            .distinctBy { it.symbol }
        MarketSnapshot(assets = assets)
    }.onFailure { e -> Log.e(TAG, "getMarketSnapshot failed", e) }

    override suspend fun getAsset(id: String): Result<CryptoAsset> = runCatching {
        api.getTicker24h(id).toDomain()
    }.onFailure { e -> Log.w(TAG, "getAsset($id) failed: ${e.message}") }

    override fun getWatchlist(): Flow<List<CryptoAsset>> =
        dao.observeWatchlist().map { entities -> entities.map { it.toDomain() } }

    override fun observeLivePrices(ids: List<String>): Flow<LivePrice> =
        streamClient.observeTickers(ids)

    override suspend fun addToWatchlist(asset: CryptoAsset) = dao.insert(asset.toEntity())

    override suspend fun removeFromWatchlist(id: String) = dao.deleteById(id)
}
