package com.example.stocktracker.data.repository

import android.util.Log
import com.example.stocktracker.data.cache.MarketSnapshotCache
import com.example.stocktracker.data.local.dao.CryptoDao
import com.example.stocktracker.data.mapper.AssetNames
import com.example.stocktracker.data.mapper.USD_STABLE_QUOTE_PRIORITY
import com.example.stocktracker.data.mapper.isUsdStableQuote
import com.example.stocktracker.data.mapper.toDomain
import com.example.stocktracker.data.mapper.toEntity
import com.example.stocktracker.data.remote.api.BinanceApi
import com.example.stocktracker.data.remote.dto.SymbolInfoDto
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import com.example.stocktracker.data.remote.websocket.BinanceStreamClient
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.repository.CryptoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BinanceCryptoRepo"
private const val MAX_SEARCH_RESULTS = 30

@Singleton
class BinanceCryptoRepositoryImpl @Inject constructor(
    private val api: BinanceApi,
    private val streamClient: BinanceStreamClient,
    private val cache: MarketSnapshotCache,
    private val dao: CryptoDao,
) : CryptoRepository {

    override suspend fun searchAssets(query: String): Result<List<CryptoAsset>> = runCatching {
        val q = query.trim().uppercase()
        if (q.isBlank()) return@runCatching emptyList()

        val pairs = cache.pairsByBaseAsset()
        val tickerBySymbol = cache.allTickers().associateBy { it.symbol }

        val candidates = pairs.keys.asSequence()
            .map { base ->
                val name = AssetNames.friendly(base)
                val symbolMatch = base.startsWith(q)
                val nameMatch = name.contains(q, ignoreCase = true)
                val symbolContains = base.contains(q)
                val score = when {
                    symbolMatch -> 3
                    nameMatch && base == q -> 3
                    nameMatch -> 2
                    symbolContains -> 1
                    else -> 0
                }
                base to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(MAX_SEARCH_RESULTS * 2)
            .toList()

        candidates.mapNotNull { base ->
            resolveBestPair(base, pairs, tickerBySymbol)
        }.take(MAX_SEARCH_RESULTS)
            .also { Log.d(TAG, "searchAssets(\"$query\") → ${it.size} results") }
    }.onFailure { Log.e(TAG, "searchAssets(\"$query\") failed", it) }

    override suspend fun getAsset(id: String): Result<CryptoAsset> = runCatching {
        val ticker = api.getTicker24h(id)
        val info = cache.symbolMetadata()[id]
        ticker.toDomain(info)
    }.onFailure { Log.w(TAG, "getAsset($id) failed: ${it.message}") }

    override fun getWatchlist(): Flow<List<CryptoAsset>> =
        dao.observeWatchlist().map { entities -> entities.map { it.toDomain() } }

    override fun observeLivePrices(ids: List<String>): Flow<LivePrice> =
        streamClient.observeTickers(ids)

    override suspend fun addToWatchlist(asset: CryptoAsset) = dao.insert(asset.toEntity())

    override suspend fun removeFromWatchlist(id: String) = dao.deleteById(id)

    private fun resolveBestPair(
        base: String,
        pairs: Map<String, List<SymbolInfoDto>>,
        tickerBySymbol: Map<String, Ticker24hDto>,
    ): CryptoAsset? {
        val candidates = pairs[base].orEmpty()
            .filter { it.quoteAsset.isUsdStableQuote() }
        if (candidates.isEmpty()) return null

        val byQuote = candidates.associateBy { it.quoteAsset }
        for (quote in USD_STABLE_QUOTE_PRIORITY) {
            val info = byQuote[quote] ?: continue
            val ticker = tickerBySymbol[info.symbol] ?: continue
            return ticker.toDomain(info)
        }
        return null
    }
}
