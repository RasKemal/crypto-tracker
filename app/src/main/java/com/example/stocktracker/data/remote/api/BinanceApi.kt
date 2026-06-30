package com.example.stocktracker.data.remote.api

import com.example.stocktracker.data.remote.dto.Ticker24hDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {

    @GET("ticker/24hr")
    suspend fun getAll24hTickers(): List<Ticker24hDto>

    @GET("ticker/24hr")
    suspend fun getTicker24h(@Query("symbol") symbol: String): Ticker24hDto
}
