package com.example.stocktracker.data.remote.api

import com.example.stocktracker.data.remote.dto.BasicFinancialsDto
import com.example.stocktracker.data.remote.dto.CompanyProfileDto
import com.example.stocktracker.data.remote.dto.StockQuoteDto
import com.example.stocktracker.data.remote.dto.StockSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

// Token is injected on every request by FinnhubAuthInterceptor — not here.
interface FinnhubApi {

    @GET("search")
    suspend fun searchSymbols(@Query("q") query: String): StockSearchResponseDto

    @GET("quote")
    suspend fun getQuote(@Query("symbol") symbol: String): StockQuoteDto

    // Returns {} for crypto or unsupported free-tier symbols.
    @GET("stock/profile2")
    suspend fun getCompanyProfile(@Query("symbol") symbol: String): CompanyProfileDto

    @GET("stock/metric")
    suspend fun getBasicFinancials(
        @Query("symbol") symbol: String,
        @Query("metric") metric: String = "all",
    ): BasicFinancialsDto
}
