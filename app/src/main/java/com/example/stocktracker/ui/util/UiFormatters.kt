package com.example.stocktracker.ui.util

import java.util.Locale
import kotlin.math.abs

internal fun Double.formatUsd(): String = when {
    this >= 1.0 -> String.format(Locale.US, "$%,.2f", this)
    this >= 0.01 -> String.format(Locale.US, "$%.4f", this)
    else -> String.format(Locale.US, "$%.6f", this)
}

internal fun Double.formatChangePercent(): String {
    val numStr = String.format(Locale("tr", "TR"), "%.2f", abs(this))
    return if (this >= 0.0) "%$numStr" else "-%$numStr"
}

internal fun Double?.formatUsdOrDash(): String = this?.formatUsd() ?: "--"

internal fun Double?.formatLargeUsdOrDash(): String = this?.formatLargeUsd() ?: "--"

internal fun Double.formatLargeUsd(): String = when {
    this >= 1e12 -> String.format(Locale.US, "$%,.2fT", this / 1e12)
    this >= 1e9 -> String.format(Locale.US, "$%,.2fB", this / 1e9)
    this >= 1e6 -> String.format(Locale.US, "$%,.2fM", this / 1e6)
    this >= 1e3 -> String.format(Locale.US, "$%,.2fK", this / 1e3)
    else -> String.format(Locale.US, "$%,.2f", this)
}
