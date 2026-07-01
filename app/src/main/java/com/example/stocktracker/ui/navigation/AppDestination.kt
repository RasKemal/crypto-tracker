package com.example.stocktracker.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import com.example.stocktracker.R
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(val route: String) {

    sealed class Tab(
        route: String,
        @StringRes val contentDescriptionRes: Int,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
    ) : AppDestination(route)

    data object Watchlist : Tab(
        route = "watchlist",
        contentDescriptionRes = R.string.nav_watchlist,
        selectedIcon = Icons.Rounded.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
    )

    data object Search : Tab(
        route = "search",
        contentDescriptionRes = R.string.nav_search,
        selectedIcon = Icons.Rounded.Search,
        unselectedIcon = Icons.Outlined.Search,
    )

    data object Detail : AppDestination(route = "detail/{id}?symbol={symbol}&name={name}") {
        const val ARG_ID = "id"
        const val ARG_SYMBOL = "symbol"
        const val ARG_NAME = "name"

        fun routeFor(id: String, symbol: String = "", name: String = ""): String =
            "detail/${Uri.encode(id)}?symbol=${Uri.encode(symbol)}&name=${Uri.encode(name)}"
    }

    companion object {
        val tabs: List<Tab> = listOf(Search, Watchlist)
    }
}
