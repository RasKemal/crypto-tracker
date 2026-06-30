package com.example.stocktracker.domain.repository

import com.example.stocktracker.domain.model.CryptoAsset

interface PopularCryptoRepository {
    suspend fun getPopularCryptos(): Result<List<CryptoAsset>>
}
