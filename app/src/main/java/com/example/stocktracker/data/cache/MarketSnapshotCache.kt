package com.example.stocktracker.data.cache

import android.util.Log
import com.example.stocktracker.data.remote.api.BinanceApi
import com.example.stocktracker.data.remote.dto.Ticker24hDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MarketCache"
private const val TICKER_TTL_MS = 20_000L

@Singleton
class MarketSnapshotCache @Inject constructor(
    private val api: BinanceApi,
) {
    private val mutex = Mutex()
    @Volatile private var cache: List<Ticker24hDto> = emptyList()
    @Volatile private var fetchedAtMs: Long = 0L

    suspend fun allTickers(forceRefresh: Boolean = false): Result<List<Ticker24hDto>> {
        if (!forceRefresh && cache.isNotEmpty() &&
            System.currentTimeMillis() - fetchedAtMs < TICKER_TTL_MS
        ) return Result.success(cache)

        return mutex.withLock {
            if (!forceRefresh && cache.isNotEmpty() &&
                System.currentTimeMillis() - fetchedAtMs < TICKER_TTL_MS
            ) return@withLock Result.success(cache)

            try {
                val fresh = api.getAll24hTickers()
                cache = fresh
                fetchedAtMs = System.currentTimeMillis()
                Log.d(TAG, "tickers refreshed (${fresh.size} symbols)")
                Result.success(fresh)
            } catch (error: Exception) {
                Log.w(TAG, "tickers refresh failed: ${error.message}")
                if (cache.isNotEmpty()) Result.success(cache)
                else Result.failure(error)
            }
        }
    }
}
