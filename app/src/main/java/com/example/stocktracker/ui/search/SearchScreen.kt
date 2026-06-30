package com.example.stocktracker.ui.search

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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.AssetListItem
import com.example.stocktracker.ui.common.LoadState
import com.example.stocktracker.ui.common.MidasSearchBar
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.model.StableAssetUiModel
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun SearchScreen(
    onAssetClick: (id: String, symbol: String, name: String) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val livePrices = viewModel.livePrices.collectAsStateWithLifecycle()

    SearchContent(
        uiState = uiState,
        livePrices = livePrices,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onEvent = { event ->
            if (event is SearchEvent.AssetClicked) {
                onAssetClick(event.asset.id, event.asset.symbol, event.asset.name)
            } else viewModel.onEvent(event)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    uiState: SearchUiState,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Keşfet",
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
            query = uiState.query,
            onQueryChange = { onEvent(SearchEvent.QueryChanged(it)) },
            placeholder = "Kripto ara",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val content = uiState.content) {
                LoadState.Loading -> SearchLoadingView()
                is LoadState.Error -> SearchErrorView(
                    message = content.message,
                    onRetry = { onEvent(SearchEvent.Retry) },
                )
                is LoadState.Success -> when {
                    content.data.isEmpty() && uiState.query.isNotBlank() ->
                        SearchEmptyView(query = uiState.query)
                    else -> AssetList(
                        header = if (uiState.isShowingPopular) "Popüler" else null,
                        assets = content.data,
                        livePrices = livePrices,
                        onAssetClick = { onEvent(SearchEvent.AssetClicked(it)) },
                        onWatchlistToggle = { onEvent(SearchEvent.WatchlistToggled(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetList(
    header: String?,
    assets: List<StableAssetUiModel>,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    onAssetClick: (StableAssetUiModel) -> Unit,
    onWatchlistToggle: (StableAssetUiModel) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (header != null) {
            item(key = "header") {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
            }
        }
        itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
            val onClick = remember(asset.id) { { onAssetClick(asset) } }
            val onToggle = remember(asset.id) { { onWatchlistToggle(asset) } }
            AssetListItem(
                asset = asset,
                livePrices = livePrices,
                onClick = onClick,
                onWatchlistToggle = onToggle,
                showDivider = index < assets.lastIndex,
            )
        }
    }
}

@Composable
private fun SearchLoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun SearchErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Tekrar dene")
        }
    }
}

@Composable
private fun SearchEmptyView(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "\"$query\" için sonuç bulunamadı",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Farklı bir terim deneyin (örn. \"bitcoin\", \"sol\").",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val previewPopular = listOf(
    StableAssetUiModel("BTCUSDT", "BTC", "Bitcoin", 1, false),
    StableAssetUiModel("ETHUSDT", "ETH", "Ethereum", 2, false),
)

@Preview(name = "Search — Popular (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchPopularDarkPreview() {
    val livePrices = remember {
        mutableStateOf(
            mapOf(
                "BTCUSDT" to PriceDisplayUiModel("\$67,320.45", "%2,45", true, 67320.45),
                "ETHUSDT" to PriceDisplayUiModel("\$3,512.10", "%1,10", true, 3512.10),
            ),
        )
    }
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(content = LoadState.Success(previewPopular)),
            livePrices = livePrices,
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Search — Error (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchErrorDarkPreview() {
    val livePrices = remember { mutableStateOf(emptyMap<String, PriceDisplayUiModel>()) }
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(content = LoadState.Error("Piyasa verisi yüklenemedi")),
            livePrices = livePrices,
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}
