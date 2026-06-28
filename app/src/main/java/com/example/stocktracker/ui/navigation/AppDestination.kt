package com.example.stocktracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

// DetailScreen is not a tab — it is a full-screen push route navigated to from list items.
enum class AppDestination(
    val route: String,
    val contentDescription: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    WATCHLIST(
        route = "watchlist",
        contentDescription = "İzleme Listesi",
        selectedIcon = Icons.Rounded.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
    ),
    SEARCH(
        route = "search",
        contentDescription = "Keşfet",
        selectedIcon = Icons.Rounded.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
}

const val DETAIL_ROUTE = "detail/{symbol}"

// URL-encode the symbol so dots and colons (e.g. THYAO.IS, BINANCE:BTCUSDT) don't break the route.
fun detailRoute(symbol: String): String =
    "detail/${android.net.Uri.encode(symbol)}"
