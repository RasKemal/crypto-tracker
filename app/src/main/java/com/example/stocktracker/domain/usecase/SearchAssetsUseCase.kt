package com.example.stocktracker.domain.usecase

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.repository.CryptoRepository
import javax.inject.Inject

private const val MAX_SEARCH_RESULTS = 30

class SearchAssetsUseCase @Inject constructor(
    private val repository: CryptoRepository,
) {
    suspend operator fun invoke(query: String): Result<List<CryptoAsset>> = runCatching {
        val q = query.trim().uppercase()
        require(q.isNotBlank()) { "Arama terimi boş olamaz" }

        val snapshot = repository.getMarketSnapshot().getOrThrow()
        snapshot.assets.asSequence()
            .map { it to scoreMatch(it, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(MAX_SEARCH_RESULTS)
            .toList()
    }

    private fun scoreMatch(asset: CryptoAsset, query: String): Int = when {
        asset.symbol.equals(query, ignoreCase = true) -> 4
        asset.symbol.startsWith(query, ignoreCase = true) -> 3
        asset.name.contains(query, ignoreCase = true) -> 2
        asset.symbol.contains(query, ignoreCase = true) -> 1
        else -> 0
    }
}
