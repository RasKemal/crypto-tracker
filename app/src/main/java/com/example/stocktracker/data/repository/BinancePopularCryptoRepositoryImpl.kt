package com.example.stocktracker.data.repository

import android.util.Log
import com.example.stocktracker.data.cache.MarketSnapshotCache
import com.example.stocktracker.data.mapper.USD_STABLE_QUOTE_PRIORITY
import com.example.stocktracker.data.mapper.isUsdStableQuote
import com.example.stocktracker.data.mapper.toDomain
import com.example.stocktracker.data.remote.dto.SymbolInfoDto
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.repository.PopularCryptoRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BinancePopularRepo"
private const val POPULAR_LIMIT = 10

@Singleton
class BinancePopularCryptoRepositoryImpl @Inject constructor(
    private val cache: MarketSnapshotCache,
) : PopularCryptoRepository {

    override suspend fun getPopularCryptos(): Result<List<CryptoAsset>> = runCatching {
        val tickers = cache.allTickers()
        val symbolInfo = cache.symbolMetadata()

        data class Ranked(val ticker: Ticker24hDto, val info: SymbolInfoDto, val quoteVol: Double)

        val ranked = tickers.asSequence()
            .mapNotNull { t ->
                val info = symbolInfo[t.symbol] ?: return@mapNotNull null
                if (!info.quoteAsset.isUsdStableQuote()) return@mapNotNull null
                val vol = t.quoteVolume?.toDoubleOrNull() ?: return@mapNotNull null
                if (vol <= 0.0) return@mapNotNull null
                Ranked(t, info, vol)
            }
            .sortedByDescending { it.quoteVol }
            .toList()

        val seen = mutableSetOf<String>()
        val picked = mutableListOf<Ranked>()
        for (r in ranked) {
            if (r.info.baseAsset in seen) continue
            picked += r
            seen += r.info.baseAsset
            if (picked.size >= POPULAR_LIMIT) break
        }

        picked.map { it.ticker.toDomain(it.info) }
            .also { Log.d(TAG, "popular → ${it.size} assets (top: ${it.firstOrNull()?.id})") }
    }.onFailure { Log.w(TAG, "getPopular failed: ${it.message}") }
}
