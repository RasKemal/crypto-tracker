package com.example.stocktracker.domain.util

val USD_STABLE_QUOTES: List<String> = listOf("USDT", "USDC", "FDUSD")

fun String.isUsdStableQuote(): Boolean = this in USD_STABLE_QUOTES

fun stripUsdStableQuote(symbol: String): String =
    USD_STABLE_QUOTES
        .firstOrNull { symbol.endsWith(it) }
        ?.let { symbol.dropLast(it.length) }
        ?: symbol
