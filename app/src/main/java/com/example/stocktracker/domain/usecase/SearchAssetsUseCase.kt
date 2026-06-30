package com.example.stocktracker.domain.usecase

import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.domain.model.MarketSnapshot
import com.example.stocktracker.domain.repository.CryptoRepository
import com.example.stocktracker.domain.util.USD_STABLE_QUOTE_PRIORITY
import com.example.stocktracker.domain.util.isUsdStableQuote
import javax.inject.Inject

private const val MAX_SEARCH_RESULTS = 30

class SearchAssetsUseCase @Inject constructor(
    private val repository: CryptoRepository,
) {
    suspend operator fun invoke(query: String): Result<List<CryptoAsset>> = runCatching {
        val q = query.trim().uppercase()
        require(q.isNotBlank()) { "Arama terimi boş olamaz" }

        val snapshot = repository.getMarketSnapshot().getOrThrow()
        searchSnapshot(q, snapshot)
    }

    private fun searchSnapshot(query: String, snapshot: MarketSnapshot): List<CryptoAsset> {
        val candidates = snapshot.pairsByBaseAsset.keys.asSequence()
            .map { base -> base to scoreMatch(base, query, snapshot) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(MAX_SEARCH_RESULTS * 2)
            .toList()

        return candidates.mapNotNull { base ->
            resolveBestPair(base, snapshot)
        }.take(MAX_SEARCH_RESULTS)
    }

    private fun scoreMatch(base: String, query: String, snapshot: MarketSnapshot): Int {
        val name = snapshot.pairsByBaseAsset[base]
            ?.firstNotNullOfOrNull { snapshot.assetsBySymbol[it.symbol]?.name }
            ?: base
        return when {
            base.startsWith(query) -> 3
            name.contains(query, ignoreCase = true) && base == query -> 3
            name.contains(query, ignoreCase = true) -> 2
            base.contains(query) -> 1
            else -> 0
        }
    }

    private fun resolveBestPair(base: String, snapshot: MarketSnapshot): CryptoAsset? {
        val candidates = snapshot.pairsByBaseAsset[base].orEmpty()
            .filter { it.quoteAsset.isUsdStableQuote() }
        if (candidates.isEmpty()) return null

        val byQuote = candidates.associateBy { it.quoteAsset }
        for (quote in USD_STABLE_QUOTE_PRIORITY) {
            val pair = byQuote[quote] ?: continue
            snapshot.assetsBySymbol[pair.symbol]?.let { return it }
        }
        return null
    }
}
