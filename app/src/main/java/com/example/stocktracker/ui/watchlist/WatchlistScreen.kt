package com.example.stocktracker.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.AssetListItem
import com.example.stocktracker.ui.common.MidasSearchBar
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.theme.MidasSecondaryText
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun WatchlistScreen(
    onAssetClick: (id: String, symbol: String, name: String) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val livePrices = viewModel.livePrices.collectAsStateWithLifecycle()

    WatchlistContent(
        uiState = uiState,
        livePrices = livePrices,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onAssetClick = { asset -> onAssetClick(asset.id, asset.symbol, asset.name) },
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistContent(
    uiState: WatchlistUiState,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAssetClick: (AssetUiModel) -> Unit,
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

        uiState.bannerMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> WatchlistLoadingView()
                uiState.isEmpty -> WatchlistEmptyView()
                uiState.items.isEmpty() -> WatchlistNoResultsView(query = uiState.localQuery)
                else -> WatchlistItemsList(
                    items = uiState.items,
                    livePrices = livePrices,
                    onItemClick = onAssetClick,
                    onRemove = { onEvent(WatchlistEvent.RemoveAsset(it.id)) },
                )
            }
        }
    }
}

@Composable
private fun WatchlistItemsList(
    items: List<AssetUiModel>,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    onItemClick: (AssetUiModel) -> Unit,
    onRemove: (AssetUiModel) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val onClick = remember(item.id) { { onItemClick(item) } }
            val onRemoveClick = remember(item.id) { { onRemove(item) } }
            AssetListItem(
                asset = item,
                livePrices = livePrices,
                onClick = onClick,
                trailingIcon = {
                    IconButton(onClick = onRemoveClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "İzleme listesinden çıkar",
                            tint = MidasSecondaryText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
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
    AssetUiModel("BTCUSDT", "BTC", "Bitcoin", 1),
    AssetUiModel("ETHUSDT", "ETH", "Ethereum", 2),
)

@Preview(name = "Watchlist — Populated (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WatchlistPopulatedDarkPreview() {
    val livePrices = remember {
        mutableStateOf(
            mapOf(
                "BTCUSDT" to PriceDisplayUiModel("\$67,320.45", "%2,45", true, 67320.45),
                "ETHUSDT" to PriceDisplayUiModel.Loading,
            ),
        )
    }
    StockTrackerTheme(darkTheme = true) {
        WatchlistContent(
            uiState = WatchlistUiState(isEmpty = false, items = previewWatchlistItems),
            livePrices = livePrices,
            isDarkTheme = true,
            onThemeToggle = {},
            onAssetClick = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Watchlist — Empty (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WatchlistEmptyDarkPreview() {
    val livePrices = remember { mutableStateOf(emptyMap<String, PriceDisplayUiModel>()) }
    StockTrackerTheme(darkTheme = true) {
        WatchlistContent(
            uiState = WatchlistUiState(isEmpty = true),
            livePrices = livePrices,
            isDarkTheme = true,
            onThemeToggle = {},
            onAssetClick = {},
            onEvent = {},
        )
    }
}
