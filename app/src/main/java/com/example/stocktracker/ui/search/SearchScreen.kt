package com.example.stocktracker.ui.search

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.AssetListItem
import com.example.stocktracker.ui.common.MidasSearchBar
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun SearchScreen(
    onAssetClick: (id: String, symbol: String, name: String) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        uiState = uiState,
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
            when {
                uiState.isLoading && uiState.assets.isEmpty() -> SearchLoadingView()
                uiState.assets.isEmpty() && uiState.query.isNotBlank() ->
                    SearchEmptyView(query = uiState.query)
                else -> AssetList(
                    header = if (uiState.isShowingPopular) "Popüler" else null,
                    assets = uiState.assets,
                    onAssetClick = { onEvent(SearchEvent.AssetClicked(it)) },
                    onWatchlistToggle = { onEvent(SearchEvent.WatchlistToggled(it)) },
                )
            }
        }
    }
}

@Composable
private fun AssetList(
    header: String?,
    assets: List<AssetUiModel>,
    onAssetClick: (AssetUiModel) -> Unit,
    onWatchlistToggle: (AssetUiModel) -> Unit,
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
            AssetListItem(
                asset = asset,
                onClick = { onAssetClick(asset) },
                onWatchlistToggle = { onWatchlistToggle(asset) },
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
    AssetUiModel("bitcoin",  "BTC",  "Bitcoin",  1, 67320.45, "\$67,320.45", "%2,45",  true,  false),
    AssetUiModel("ethereum", "ETH",  "Ethereum", 2, 3512.10,  "\$3,512.10",  "%1,10",  true,  false),
    AssetUiModel("solana",   "SOL",  "Solana",   3, 147.85,   "\$147.85",    "%3,78",  true,  true),
    AssetUiModel("xrp",      "XRP",  "XRP",      4, 0.5512,   "\$0.5512",    "-%1,20", false, false),
    AssetUiModel("dogecoin", "DOGE", "Dogecoin", 5, 0.1623,   "\$0.1623",    "-%2,10", false, false),
)

private val previewSearchResults = listOf(
    AssetUiModel("bitcoin",     "BTC", "Bitcoin",      1, 67320.45, "\$67,320.45", "%2,45",  true,  true),
    AssetUiModel("bitcoin-cash","BCH", "Bitcoin Cash", 2, 412.30,   "\$412.30",    "%0,85",  true,  false),
    AssetUiModel("bitcoin-sv",  "BSV", "Bitcoin SV",   3, 58.12,    "\$58.12",     "-%1,30", false, false),
)

@Preview(name = "Search — Popular (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchPopularDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "", isShowingPopular = true, assets = previewPopular, isLoading = false),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Search — Results (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchResultsDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "bitcoin", isShowingPopular = false, assets = previewSearchResults, isLoading = false),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Search — Loading (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchLoadingDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "btc", isLoading = true),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Search — No Results (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SearchNoResultsDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "xyz123", isShowingPopular = false, assets = emptyList(), isLoading = false),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(name = "Search — Popular (Light)", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun SearchPopularLightPreview() {
    StockTrackerTheme(darkTheme = false) {
        SearchContent(
            uiState = SearchUiState(query = "", isShowingPopular = true, assets = previewPopular, isLoading = false),
            isDarkTheme = false,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}
