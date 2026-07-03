package com.example.stocktracker.data.repository

import android.util.Log
import com.example.stocktracker.data.local.dao.WatchlistDao
import com.example.stocktracker.data.local.dao.MarketAssetDao
import com.example.stocktracker.data.mapper.toDomain
import com.example.stocktracker.data.mapper.toWatchlistEntity
import com.example.stocktracker.data.mapper.toMarketEntity
import com.example.stocktracker.data.remote.api.BinanceApi
import com.example.stocktracker.data.remote.websocket.BinanceStreamClient
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BinanceCryptoRepo"
private const val MARKET_TTL_MS = 3_600_000L

@Singleton
class BinanceCryptoRepositoryImpl @Inject constructor(
    private val api: BinanceApi,
    private val streamClient: BinanceStreamClient,
    private val marketAssetDao: MarketAssetDao,
    private val dao: WatchlistDao,
    private val preferences: AppPreferencesRepository,
) : CryptoRepository {

    override suspend fun getPopularAssets(): Result<List<CryptoAsset>> = runCatching {
        refreshMarketIfStale()
        marketAssetDao.getPopularAssets().map { it.toDomain() }
    }.onFailure { e -> Log.e(TAG, "getPopularAssets failed", e) }

    override suspend fun searchAssets(query: String): Result<List<CryptoAsset>> = runCatching {
        refreshMarketIfStale()
        marketAssetDao.searchAssets(query).map { it.toDomain() }
    }.onFailure { e -> Log.e(TAG, "searchAssets(\"$query\") failed", e) }

    private suspend fun refreshMarketIfStale() {
        val cached = marketAssetDao.getAll()
        val lastFetched = preferences.getMarketLastFetchedAt()
        val isStale = System.currentTimeMillis() - lastFetched > MARKET_TTL_MS
        if (cached.isNotEmpty() && !isStale) return

        try {
            val tickers = api.getAll24hTickers()
            val entities = tickers.mapNotNull { it.toMarketEntity() }
                .sortedByDescending { it.volumeUsd24Hr ?: 0.0 }
                .distinctBy { it.symbol }
            marketAssetDao.deleteAll()
            marketAssetDao.insertAll(entities)
            preferences.setMarketLastFetchedAt(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w(TAG, "market refresh failed: ${e.message}")
            if (cached.isEmpty()) throw e
        }
    }

    override suspend fun getAsset(id: String): Result<CryptoAsset> = runCatching {
        api.getTicker24h(id).toDomain()
    }.onFailure { e -> Log.w(TAG, "getAsset($id) failed: ${e.message}") }

    override fun getWatchlist(): Flow<List<CryptoAsset>> =
        dao.observeWatchlist().map { entities ->
            entities.map { entity ->
                marketAssetDao.getById(entity.id)?.toDomain() ?: entity.toDomain()
            }
        }

    override fun observeLivePrices(ids: List<String>): Flow<LivePrice> =
        streamClient.observeTickers(ids)

    override suspend fun addToWatchlist(asset: CryptoAsset) = dao.insert(asset.toWatchlistEntity())

    override suspend fun removeFromWatchlist(id: String) = dao.deleteById(id)
}
