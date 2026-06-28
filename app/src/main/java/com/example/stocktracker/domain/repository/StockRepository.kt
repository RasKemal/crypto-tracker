package com.example.stocktracker.domain.repository

import com.example.stocktracker.domain.model.BasicFinancials
import com.example.stocktracker.domain.model.CompanyProfile
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote
import kotlinx.coroutines.flow.Flow

interface StockRepository {

    suspend fun searchStocks(query: String): Result<List<Stock>>

    suspend fun getStockQuote(symbol: String): Result<StockQuote>

    // Emits a new list whenever the Room watchlist table changes.
    // Prices are NOT stored here — merge with observeLivePrices via combine in the ViewModel.
    fun getWatchlist(): Flow<List<Stock>>

    // Cold flow: opens a WebSocket when collected, closes it when cancelled.
    // Must never be collected from the data layer — the ViewModel owns the lifecycle.
    fun observeLivePrices(symbols: List<String>): Flow<LivePrice>

    suspend fun getCompanyProfile(symbol: String): Result<CompanyProfile>

    suspend fun getBasicFinancials(symbol: String): Result<BasicFinancials>

    suspend fun addToWatchlist(stock: Stock)

    suspend fun removeFromWatchlist(symbol: String)
}
