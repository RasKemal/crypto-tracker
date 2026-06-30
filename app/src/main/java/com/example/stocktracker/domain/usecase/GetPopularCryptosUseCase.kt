package com.example.stocktracker.domain.usecase

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.MarketSnapshot
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.util.isUsdStableQuote
import javax.inject.Inject

private const val POPULAR_LIMIT = 10

class GetPopularCryptosUseCase @Inject constructor(
    private val repository: CryptoRepository,
) {
    suspend operator fun invoke(): Result<List<CryptoAsset>> = runCatching {
        val snapshot = repository.getMarketSnapshot().getOrThrow()
        rankPopular(snapshot)
    }

    private fun rankPopular(snapshot: MarketSnapshot): List<CryptoAsset> {
        val ranked = snapshot.assetsBySymbol.values.asSequence()
            .filter { it.quoteAsset.isUsdStableQuote() }
            .mapNotNull { asset ->
                val vol = asset.volumeUsd24Hr ?: return@mapNotNull null
                if (vol <= 0.0) return@mapNotNull null
                asset to vol
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()

        val seen = mutableSetOf<String>()
        return buildList {
            for (asset in ranked) {
                if (asset.symbol in seen) continue
                add(asset)
                seen += asset.symbol
                if (size >= POPULAR_LIMIT) break
            }
        }
    }
}
