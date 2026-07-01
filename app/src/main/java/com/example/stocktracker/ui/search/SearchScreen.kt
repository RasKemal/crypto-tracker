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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.R
import com.example.stocktracker.ui.common.AssetListItem
import com.example.stocktracker.ui.common.LoadState
import com.example.stocktracker.ui.common.UiMessage
import com.example.stocktracker.ui.common.asString
import com.example.stocktracker.ui.common.CryptoSearchBar
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.theme.CryptoGreen
import com.example.stocktracker.ui.theme.CryptoSecondaryText
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
        onAssetClick = { asset -> onAssetClick(asset.id, asset.symbol, asset.name) },
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    uiState: SearchUiState,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAssetClick: (AssetUiModel) -> Unit,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.search_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            actions = {
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = stringResource(R.string.action_toggle_theme),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        CryptoSearchBar(
            query = uiState.query,
            onQueryChange = { onEvent(SearchEvent.QueryChanged(it)) },
            placeholder = stringResource(R.string.search_placeholder),
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
                        header = if (uiState.isShowingPopular) {
                            stringResource(R.string.search_popular_header)
                        } else {
                            null
                        },
                        assets = content.data,
                        livePrices = livePrices,
                        onAssetClick = onAssetClick,
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
    assets: List<AssetUiModel>,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
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
            val onClick = remember(asset.id) { { onAssetClick(asset) } }
            val onToggle = remember(asset.id) { { onWatchlistToggle(asset) } }
            AssetListItem(
                asset = asset,
                livePrices = livePrices,
                onClick = onClick,
                trailingIcon = {
                    IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (asset.isInWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = stringResource(
                                if (asset.isInWatchlist) {
                                    R.string.action_watchlist_remove
                                } else {
                                    R.string.action_watchlist_add
                                },
                            ),
                            tint = if (asset.isInWatchlist) CryptoGreen else CryptoSecondaryText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
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
private fun SearchErrorView(message: UiMessage, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
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
            text = stringResource(R.string.search_no_results, query),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.search_no_results_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val previewPopular = listOf(
    AssetUiModel("BTCUSDT", "BTC", "Bitcoin", false),
    AssetUiModel("ETHUSDT", "ETH", "Ethereum", false),
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
            onAssetClick = {},
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
            uiState = SearchUiState(
                content = LoadState.Error(UiMessage.Resource(R.string.error_market_data)),
            ),
            livePrices = livePrices,
            isDarkTheme = true,
            onThemeToggle = {},
            onAssetClick = {},
            onEvent = {},
        )
    }
}
