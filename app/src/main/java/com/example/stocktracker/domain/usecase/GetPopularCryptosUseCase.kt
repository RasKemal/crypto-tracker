package com.example.stocktracker.domain.usecase

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.repository.CryptoRepository
import javax.inject.Inject

private const val POPULAR_LIMIT = 10

class GetPopularCryptosUseCase @Inject constructor(
    private val repository: CryptoRepository,
) {
    suspend operator fun invoke(): Result<List<CryptoAsset>> = runCatching {
        val assets = repository.getMarketSnapshot().getOrThrow()
        assets.asSequence()
            .filter { (it.volumeUsd24Hr ?: 0.0) > 0.0 }
            .take(POPULAR_LIMIT)
            .toList()
    }
}
