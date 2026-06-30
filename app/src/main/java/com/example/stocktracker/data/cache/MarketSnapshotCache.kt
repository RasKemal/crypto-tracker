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
    @Volatile private var exchangeLastError: Throwable? = null

    suspend fun allTickers(forceRefresh: Boolean = false): Result<List<Ticker24hDto>> {
        if (!forceRefresh && tickerCache.isNotEmpty() &&
            System.currentTimeMillis() - tickerFetchedAtMs < TICKER_TTL_MS
        ) return Result.success(tickerCache)

        return tickerMutex.withLock {
            if (!forceRefresh && tickerCache.isNotEmpty() &&
                System.currentTimeMillis() - tickerFetchedAtMs < TICKER_TTL_MS
            ) return@withLock Result.success(tickerCache)

            try {
                val fresh = api.getAll24hTickers()
                tickerCache = fresh
                tickerFetchedAtMs = System.currentTimeMillis()
                Log.d(TAG, "tickers refreshed (${fresh.size} symbols)")
                Result.success(fresh)
            } catch (error: Exception) {
                Log.w(TAG, "tickers refresh failed: ${error.message}")
                if (tickerCache.isNotEmpty()) Result.success(tickerCache)
                else Result.failure(error)
            }
        }
    }

    suspend fun symbolMetadata(): Result<Map<String, SymbolInfoDto>> {
        ensureExchangeInfo()
        return if (symbolInfoBySymbol.isNotEmpty()) {
            Result.success(symbolInfoBySymbol)
        } else {
            Result.failure(
                exchangeLastError ?: IllegalStateException("Sembol kataloğu yüklenemedi"),
            )
        }
    }

    suspend fun pairsByBaseAsset(): Result<Map<String, List<SymbolInfoDto>>> {
        ensureExchangeInfo()
        return if (symbolInfoByBase.isNotEmpty()) {
            Result.success(symbolInfoByBase)
        } else {
            Result.failure(
                exchangeLastError ?: IllegalStateException("Sembol kataloğu yüklenemedi"),
            )
        }
    }

    private suspend fun ensureExchangeInfo() {
        if (symbolInfoBySymbol.isNotEmpty() &&
            System.currentTimeMillis() - exchangeFetchedAtMs < EXCHANGE_INFO_TTL_MS
        ) return

        exchangeMutex.withLock {
            if (symbolInfoBySymbol.isNotEmpty() &&
                System.currentTimeMillis() - exchangeFetchedAtMs < EXCHANGE_INFO_TTL_MS
            ) return@withLock

            try {
                val info = api.getExchangeInfo()
                val trading = info.symbols.filter { it.status.equals("TRADING", ignoreCase = true) }
                symbolInfoBySymbol = trading.associateBy { it.symbol }
                symbolInfoByBase = trading.groupBy { it.baseAsset }
                exchangeFetchedAtMs = System.currentTimeMillis()
                exchangeLastError = null
                Log.d(TAG, "exchangeInfo refreshed (${trading.size} TRADING symbols)")
            } catch (error: Exception) {
                exchangeLastError = error
                Log.w(TAG, "exchangeInfo refresh failed: ${error.message}")
            }
        }
    }
}
