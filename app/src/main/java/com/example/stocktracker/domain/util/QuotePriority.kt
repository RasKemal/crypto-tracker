package com.example.stocktracker.domain.util

val USD_STABLE_QUOTE_PRIORITY: List<String> = listOf("USDT", "USDC", "FDUSD")

fun String.isUsdStableQuote(): Boolean = this in USD_STABLE_QUOTE_PRIORITY

fun stripUsdStableQuote(symbol: String): String =
    USD_STABLE_QUOTE_PRIORITY
        .firstOrNull { symbol.endsWith(it) }
        ?.let { symbol.dropLast(it.length) }
        ?: symbol
