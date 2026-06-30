package com.example.stocktracker.data.mapper

internal val USD_STABLE_QUOTE_PRIORITY: List<String> = listOf("USDT", "USDC", "FDUSD")

internal fun String.isUsdStableQuote(): Boolean = this in USD_STABLE_QUOTE_PRIORITY

internal fun stripUsdStableQuote(symbol: String): String =
    USD_STABLE_QUOTE_PRIORITY
        .firstOrNull { symbol.endsWith(it) }
        ?.let { symbol.dropLast(it.length) }
        ?: symbol
