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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.MarketFilterChips
import com.example.stocktracker.ui.common.MidasSearchBar
import com.example.stocktracker.ui.common.StockListItem
import com.example.stocktracker.ui.model.StockUiModel
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun SearchScreen(
    onStockClick: (String) -> Unit,
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
            if (event is SearchEvent.StockClicked) onStockClick(event.symbol)
            else viewModel.onEvent(event)
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
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        MarketFilterChips(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = { onEvent(SearchEvent.FilterSelected(it)) },
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> SearchLoadingView()
                uiState.query.isEmpty() -> SearchIdleView()
                uiState.stocks.isEmpty() -> SearchEmptyView(query = uiState.query)
                else -> SearchResultsList(
                    stocks = uiState.stocks,
                    onStockClick = { onEvent(SearchEvent.StockClicked(it)) },
                    onWatchlistToggle = { onEvent(SearchEvent.WatchlistToggled(it)) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    stocks: List<StockUiModel>,
    onStockClick: (String) -> Unit,
    onWatchlistToggle: (StockUiModel) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(stocks, key = { _, stock -> stock.symbol }) { index, stock ->
            StockListItem(
                stock = stock,
                onClick = { onStockClick(stock.symbol) },
                onWatchlistToggle = { onWatchlistToggle(stock) },
                showDivider = index < stocks.lastIndex,
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
private fun SearchIdleView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Hisse senedi, ETF veya kripto ara",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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
            text = "Farklı bir arama terimi veya piyasa filtresi deneyin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val previewStocks = listOf(
    StockUiModel("SPCE", "SPCE", "Virgin Galactic Holdings Inc - Class A",  1, "\$4.78",   "%18,00", true,  false),
    StockUiModel("RXT",  "RXT",  "Rackspace Technology",                     2, "\$2.14",   "-%5,97", false, true),
    StockUiModel("LVWR", "LVWR", "LiveWire Group, Inc.",                     3, "\$1.32",   "%36,63", true,  false),
    StockUiModel("USAS", "USAS", "Americas Gold and Silver",                 4, "\$0.92",   "%0,42",  true,  true),
    StockUiModel("EOSE", "EOSE", "Eos Energy Enterprises Inc - Class A",     5, "\$3.40",   "-%2,63", false, false),
    StockUiModel("AIIO", "AIIO", "Robo.ai Inc. Class B Ordinary Shares",     6, "\$5.10",   "%12,05", true,  false),
    StockUiModel("ONDS", "ONDS", "Ondas Holdings Inc",                       7, "\$0.74",   "%2,02",  true,  false),
    StockUiModel("POET", "POET", "POET Technologies Inc. Common Shares",     8, "\$4.22",   "-%6,81", false, false),
)

@Preview(
    name = "Search — Results (Dark)",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = false,
)
@Composable
private fun SearchResultsDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(
                query = "space",
                selectedFilter = com.example.stocktracker.domain.model.MarketFilter.ABD,
                stocks = previewStocks,
                isLoading = false,
            ),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Search — Idle / No Query (Dark)",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun SearchIdleDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = ""),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Search — Loading (Dark)",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun SearchLoadingDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "apple", isLoading = true),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Search — No Results (Dark)",
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun SearchNoResultsDarkPreview() {
    StockTrackerTheme(darkTheme = true) {
        SearchContent(
            uiState = SearchUiState(query = "xyzabc123", stocks = emptyList(), isLoading = false),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Search — Results (Light)",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun SearchResultsLightPreview() {
    StockTrackerTheme(darkTheme = false) {
        SearchContent(
            uiState = SearchUiState(
                query = "space",
                selectedFilter = com.example.stocktracker.domain.model.MarketFilter.ABD,
                stocks = previewStocks,
                isLoading = false,
            ),
            isDarkTheme = false,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}

@Preview(
    name = "Search — BİST Filter (Light)",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun SearchBistLightPreview() {
    StockTrackerTheme(darkTheme = false) {
        SearchContent(
            uiState = SearchUiState(
                query = "thyao",
                selectedFilter = com.example.stocktracker.domain.model.MarketFilter.BIST,
                stocks = listOf(
                    StockUiModel("THYAO.IS", "THYAO", "Türk Hava Yolları A.O.", 1, "₺312.00", "%4,20",  true,  true),
                    StockUiModel("ASELS.IS", "ASELS", "Aselsan Elektronik",     2, "₺85.40",  "-%1,35", false, false),
                    StockUiModel("EREGL.IS", "EREGL", "Ereğli Demir ve Çelik",  3, "₺48.20",  "%0,87",  true,  false),
                ),
                isLoading = false,
            ),
            isDarkTheme = false,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}
