package com.example.stocktracker.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.StockAvatar
import com.example.stocktracker.ui.theme.MidasGreen
import com.example.stocktracker.ui.theme.MidasRed
import com.example.stocktracker.ui.theme.MidasSecondaryText
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailContent(
        uiState = uiState,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onEvent = { event ->
            if (event is DetailEvent.NavigateBack) onBack()
            else viewModel.onEvent(event)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    uiState: DetailUiState,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.displaySymbol.ifBlank { uiState.symbol },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(DetailEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(DetailEvent.ToggleWatchlist) }) {
                        Icon(
                            imageVector = if (uiState.isInWatchlist) {
                                Icons.Rounded.Bookmark
                            } else {
                                Icons.Rounded.BookmarkBorder
                            },
                            contentDescription = if (uiState.isInWatchlist) "Listeden çıkar" else "Listeye ekle",
                            tint = if (uiState.isInWatchlist) MidasGreen else MaterialTheme.colorScheme.onBackground,
                        )
                    }
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
        },
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.formattedPrice == "--" -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            else -> {
                DetailBody(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun DetailBody(uiState: DetailUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PriceHeader(uiState = uiState)

        if (uiState.formattedOpen != "--") {
            DetailSection(title = "Piyasa Özeti") {
                StatGrid(
                    stats = listOf(
                        "Açılış" to uiState.formattedOpen,
                        "Gün Yüksek" to uiState.formattedHigh,
                        "Gün Düşük" to uiState.formattedLow,
                        "Önceki Kapanış" to uiState.formattedPrevClose,
                    )
                )
            }
        }

        if (uiState.formatted52WHigh != "--" || uiState.formattedPeRatio != "--") {
            DetailSection(title = "Finansal Metrikler") {
                StatGrid(
                    stats = buildList {
                        if (uiState.formatted52WHigh != "--") add("52H Yüksek" to uiState.formatted52WHigh)
                        if (uiState.formatted52WLow != "--") add("52H Düşük" to uiState.formatted52WLow)
                        if (uiState.formattedPeRatio != "--") add("F/K Oranı" to uiState.formattedPeRatio)
                        if (uiState.formattedBeta != "--") add("Beta" to uiState.formattedBeta)
                        if (uiState.formattedDividendYield != "--") add("Temettü Verimi" to uiState.formattedDividendYield)
                        if (uiState.formattedMarketCap != "--") add("Piyasa Değeri" to uiState.formattedMarketCap)
                    }
                )
            }
        }

        if (uiState.companyName.isNotBlank() && uiState.companyName != uiState.symbol) {
            DetailSection(title = "Şirket Bilgisi") {
                CompanyInfo(uiState = uiState)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PriceHeader(uiState: DetailUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StockAvatar(symbol = uiState.displaySymbol.ifBlank { uiState.symbol }, size = 52.dp)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (uiState.companyName.isNotBlank() && uiState.companyName != uiState.symbol) {
                Text(
                    text = uiState.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (uiState.industry.isNotBlank()) {
                Text(
                    text = uiState.industry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    AnimatedContent(
        targetState = uiState.formattedPrice,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "price",
    ) { price ->
        Text(
            text = price,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 36.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val color = if (uiState.isPositive) MidasGreen else MidasRed
        Text(
            text = uiState.formattedChange,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "(${uiState.formattedChangePercent})",
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp,
        )
        content()
    }
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    val rows = stats.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompanyInfo(uiState: DetailUiState) {
    val info = buildList {
        if (uiState.country.isNotBlank()) add("Ülke" to uiState.country)
        if (uiState.currency.isNotBlank()) add("Para Birimi" to uiState.currency)
        if (uiState.ipo.isNotBlank()) add("Halka Arz" to uiState.ipo)
    }
    if (info.isEmpty()) return
    StatGrid(stats = info)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DetailContentPreview() {
    StockTrackerTheme(darkTheme = true) {
        DetailContent(
            uiState = DetailUiState(
                symbol = "AAPL",
                displaySymbol = "AAPL",
                companyName = "Apple Inc.",
                industry = "Technology",
                country = "US",
                currency = "USD",
                ipo = "1980-12-12",
                formattedPrice = "$189.45",
                formattedChange = "+$2.30",
                formattedChangePercent = "%1,23",
                isPositive = true,
                formattedOpen = "$187.15",
                formattedHigh = "$190.23",
                formattedLow = "$186.50",
                formattedPrevClose = "$187.15",
                formatted52WHigh = "$199.62",
                formatted52WLow = "$124.17",
                formattedPeRatio = "30.35",
                formattedBeta = "1.20",
                formattedDividendYield = "0.49%",
                formattedMarketCap = "$2.89T",
                isInWatchlist = true,
                isLoading = false,
            ),
            isDarkTheme = true,
            onThemeToggle = {},
            onEvent = {},
        )
    }
}
