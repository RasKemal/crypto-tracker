package com.example.stocktracker.data.repository

import android.util.Log
import com.example.stocktracker.data.local.dao.StockDao
import com.example.stocktracker.data.mapper.toDomain
import com.example.stocktracker.data.mapper.toEntity
import com.example.stocktracker.data.remote.api.FinnhubApi
import com.example.stocktracker.data.remote.websocket.FinnhubWebSocketClient
import com.example.stocktracker.domain.model.BasicFinancials
import com.example.stocktracker.domain.model.CompanyProfile
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote
import com.example.stocktracker.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StockRepo"

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: FinnhubApi,
    private val webSocketClient: FinnhubWebSocketClient,
    private val stockDao: StockDao,
) : StockRepository {

    override suspend fun searchStocks(query: String): Result<List<Stock>> =
        runCatching {
            val raw = api.searchSymbols(query).result
            Log.d(TAG, "searchSymbols(\"$query\") → ${raw.size} dto results")
            raw.map { dto ->
                dto.toDomain(isInWatchlist = stockDao.isInWatchlist(dto.symbol))
            }
        }.onFailure { Log.e(TAG, "searchSymbols failed", it) }

    override suspend fun getStockQuote(symbol: String): Result<StockQuote> =
        runCatching { api.getQuote(symbol).toDomain(symbol) }
            .onFailure { Log.w(TAG, "getQuote($symbol) failed: ${it.message}") }

    override suspend fun getCompanyProfile(symbol: String): Result<CompanyProfile> =
        runCatching { api.getCompanyProfile(symbol).toDomain(symbol) }

    override suspend fun getBasicFinancials(symbol: String): Result<BasicFinancials> =
        runCatching { api.getBasicFinancials(symbol).toDomain() }

    override fun getWatchlist(): Flow<List<Stock>> =
        stockDao.observeWatchlist().map { entities -> entities.map { it.toDomain() } }

    override fun observeLivePrices(symbols: List<String>): Flow<LivePrice> =
        webSocketClient.observePrices(symbols)

    override suspend fun addToWatchlist(stock: Stock) =
        stockDao.insert(stock.toEntity())

    override suspend fun removeFromWatchlist(symbol: String) =
        stockDao.deleteBySymbol(symbol)
}
