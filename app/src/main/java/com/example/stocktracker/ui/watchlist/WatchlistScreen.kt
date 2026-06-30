package com.example.stocktracker.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.MidasSearchBar
import com.example.stocktracker.ui.common.WatchlistListItem
import com.example.stocktracker.ui.model.WatchlistItemUiModel
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun WatchlistScreen(
    onAssetClick: (id: String, symbol: String, name: String) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WatchlistContent(
        uiState = uiState,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onEvent = { event ->
            if (event is WatchlistEvent.AssetClicked) {
                onAssetClick(event.item.id, event.item.symbol, event.item.name)
            } else viewModel.onEvent(event)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistContent(
    uiState: WatchlistUiState,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onEvent: (WatchlistEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "İzleme Listesi",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            actions = {
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Tema değiştir",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        MidasSearchBar(
            query = uiState.localQuery,
            onQueryChange = { onEvent(WatchlistEvent.LocalQueryChanged(it)) },
            placeholder = "İzleme listesinde ara",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading       -> WatchlistLoadingView()
                uiState.isEmpty         -> WatchlistEmptyView()
                uiState.items.isEmpty() -> WatchlistNoResultsView(query = uiState.localQuery)
                else -> WatchlistItemsList(
                    items = uiState.items,
                    onItemClick = { onEvent(WatchlistEvent.AssetClicked(it)) },
                    onRemove = { onEvent(WatchlistEvent.RemoveAsset(it.id)) },
                )
            }
        }
    }
}

@Composable
private fun WatchlistItemsList(
    items: List<WatchlistItemUiModel>,
    onItemClick: (WatchlistItemUiModel) -> Unit,
    onRemove: (WatchlistItemUiModel) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            WatchlistListItem(
                item = item,
                onClick = { onItemClick(item) },
                onRemove = { onRemove(item) },
                showDivider = index < items.lastIndex,
            )
        }
    }
}

@Composable
private fun WatchlistLoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun WatchlistEmptyView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.BookmarkBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "İzleme listeniz boş",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Keşfet ekranından kripto ekleyerek\nfiyatları buradan takip edebilirsiniz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WatchlistNoResultsView(query: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\"$query\" için sonuç bulunamadı",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val previewWatchlistItems = listOf(
    WatchlistItemUiModel("bitcoin",  "BTC",  "Bitcoin",  1, 67320.45, "\$67,320.45", "%2,45",  true),
    WatchlistItemUiModel("ethereum", "ETH",  "Ethereum", 2, 3512.10,  "\$3,512.10",  "%1,10",  true),
    WatchlistItemUiModel("solana",   "SOL",  "Solana",   3, 147.85,   "\$147.85",    "%3,78",  true),
    WatchlistItemUiModel("xrp",      "XRP",  "XRP",      4, 0.5512,   "\$0.5512",    "-%1,20", false),
    WatchlistItemUiModel("dogecoin", "DOGE", "Dogecoin", 5, 0.1623,   "\$0.1623",    "-%2,10", false),
)

@Preview(name = "Watchlist — Populated (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WatchlistPopulatedDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        WatchlistContent(
            uiState = WatchlistUiState(isLoading = false, isEmpty = false, items = previewWatchlistItems),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Watchlist — Local Search Active (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WatchlistLocalSearchDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        WatchlistContent(
            uiState = WatchlistUiState(
                localQuery = "btc",
                isLoading = false,
                isEmpty = false,
                items = previewWatchlistItems.filter {
                    it.id.contains("bitcoin") || it.symbol.contains("BTC", ignoreCase = true)
                },
            ),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Watchlist — Empty (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WatchlistEmptyDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        WatchlistContent(
            uiState = WatchlistUiState(isLoading = false, isEmpty = true),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Watchlist — Populated (Light)", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun WatchlistPopulatedLightPreview() {
    StockTrackerTheme(darkTheme = false) {
        WatchlistContent(
            uiState = WatchlistUiState(isLoading = false, isEmpty = false, items = previewWatchlistItems),
            isDarkTheme = false,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}
