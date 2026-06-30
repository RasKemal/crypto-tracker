package com.example.stocktracker.data.cache

import android.util.Log
import com.example.stocktracker.data.remote.api.BinanceApi
import com.example.stocktracker.data.remote.dto.SymbolInfoDto
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MarketCache"

private const val TICKER_TTL_MS = 20_000L
private const val EXCHANGE_INFO_TTL_MS = 60L * 60L * 1_000L

@Singleton
class MarketSnapshotCache @Inject constructor(
    private val api: BinanceApi,
) {
    private val tickerMutex = Mutex()
    @Volatile private var tickerCache: List<Ticker24hDto> = emptyList()
    @Volatile private var tickerFetchedAtMs: Long = 0L

    private val exchangeMutex = Mutex()
    @Volatile private var symbolInfoBySymbol: Map<String, SymbolInfoDto> = emptyMap()
    @Volatile private var symbolInfoByBase: Map<String, List<SymbolInfoDto>> = emptyMap()
    @Volatile private var exchangeFetchedAtMs: Long = 0L

    suspend fun allTickers(forceRefresh: Boolean = false): List<Ticker24hDto> {
        if (!forceRefresh && tickerCache.isNotEmpty() &&
            System.currentTimeMillis() - tickerFetchedAtMs < TICKER_TTL_MS
        ) return tickerCache

        return tickerMutex.withLock {
            if (!forceRefresh && tickerCache.isNotEmpty() &&
                System.currentTimeMillis() - tickerFetchedAtMs < TICKER_TTL_MS
            ) return@withLock tickerCache

            runCatching { api.getAll24hTickers() }
                .onSuccess { fresh ->
                    tickerCache = fresh
                    tickerFetchedAtMs = System.currentTimeMillis()
                    Log.d(TAG, "tickers refreshed (${fresh.size} symbols)")
                }
                .onFailure { Log.w(TAG, "tickers refresh failed: ${it.message}") }
            tickerCache
        }
    }

    suspend fun symbolMetadata(): Map<String, SymbolInfoDto> {
        ensureExchangeInfo()
        return symbolInfoBySymbol
    }

    suspend fun pairsByBaseAsset(): Map<String, List<SymbolInfoDto>> {
        ensureExchangeInfo()
        return symbolInfoByBase
    }

    private suspend fun ensureExchangeInfo() {
        if (symbolInfoBySymbol.isNotEmpty() &&
            System.currentTimeMillis() - exchangeFetchedAtMs < EXCHANGE_INFO_TTL_MS
        ) return

        exchangeMutex.withLock {
            if (symbolInfoBySymbol.isNotEmpty() &&
                System.currentTimeMillis() - exchangeFetchedAtMs < EXCHANGE_INFO_TTL_MS
            ) return@withLock

            runCatching { api.getExchangeInfo() }
                .onSuccess { info ->
                    val trading = info.symbols.filter { it.status.equals("TRADING", ignoreCase = true) }
                    symbolInfoBySymbol = trading.associateBy { it.symbol }
                    symbolInfoByBase = trading.groupBy { it.baseAsset }
                    exchangeFetchedAtMs = System.currentTimeMillis()
                    Log.d(TAG, "exchangeInfo refreshed (${trading.size} TRADING symbols)")
                }
                .onFailure { Log.w(TAG, "exchangeInfo refresh failed: ${it.message}") }
        }
    }
}
