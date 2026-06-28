package com.example.stocktracker.domain.model

// Classification uses Finnhub symbol patterns because the free-tier /search response
// does not populate primaryExchange — see DOCUMENTATION.md for the full rules.
enum class MarketFilter(val label: String) {
    ABD("ABD"),
    BIST("BİST"),
    AVRUPA("Avrupa"),
    KRIPTO("Kripto");

    fun matches(symbol: String): Boolean {
        if (symbol.isBlank()) return false
        val upper = symbol.uppercase()
        return when (this) {
            ABD    -> !upper.contains(':') && !upper.contains('.')
            BIST   -> upper.endsWith(".IS")
            AVRUPA -> EUROPEAN_SUFFIXES.any { upper.endsWith(it) } && !upper.endsWith(".IS")
            KRIPTO -> upper.contains(':')
        }
    }

    companion object {
        private val EUROPEAN_SUFFIXES = listOf(
            ".L", ".PA", ".F", ".DE", ".MI", ".AS", ".SW",
            ".ST", ".CO", ".OL", ".HE", ".BR", ".LS",
            ".VI", ".MC", ".IR", ".AT", ".WA", ".PR",
        )
    }
}
