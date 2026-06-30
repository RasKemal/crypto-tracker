package com.example.stocktracker.data.repository

import android.util.Log
import com.example.stocktracker.data.cache.MarketSnapshotCache
import com.example.stocktracker.data.local.dao.CryptoDao
import com.example.stocktracker.data.mapper.toCryptoPair
import com.example.stocktracker.data.mapper.toDomain
import com.example.stocktracker.data.mapper.toEntity
import com.example.stocktracker.data.remote.api.BinanceApi
import com.example.stocktracker.data.remote.websocket.BinanceStreamClient
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.MarketSnapshot
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.util.userMessage
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
        val metadata = cache.symbolMetadata().getOrThrow()
        val pairsByBase = cache.pairsByBaseAsset().getOrThrow()

        val assetsBySymbol = tickers.associate { ticker ->
            val info = metadata[ticker.symbol]
            ticker.symbol to ticker.toDomain(info)
        }

        MarketSnapshot(
            assetsBySymbol = assetsBySymbol,
            pairsByBaseAsset = pairsByBase.mapValues { (_, pairs) ->
                pairs.map { it.toCryptoPair() }
            },
        )
    }.recoverCatching { e ->
        Log.e(TAG, "getMarketSnapshot failed", e)
        error(e.userMessage("Piyasa verisi yüklenemedi"))
    }

    override suspend fun getAsset(id: String): Result<CryptoAsset> = runCatching {
        val ticker = api.getTicker24h(id)
        val info = cache.symbolMetadata().getOrNull()?.get(id)
        ticker.toDomain(info)
    }.recoverCatching { e ->
        Log.w(TAG, "getAsset($id) failed: ${e.message}")
        error(e.userMessage("Varlık verisi yüklenemedi"))
    }

    override fun getWatchlist(): Flow<List<CryptoAsset>> =
        dao.observeWatchlist().map { entities -> entities.map { it.toDomain() } }

    override fun observeLivePrices(ids: List<String>): Flow<LivePrice> =
        streamClient.observeTickers(ids)

    override suspend fun addToWatchlist(asset: CryptoAsset) = dao.insert(asset.toEntity())

    override suspend fun removeFromWatchlist(id: String) = dao.deleteById(id)
}
