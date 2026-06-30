package com.example.stocktracker.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stocktracker.ui.common.AnimatedPrice
import com.example.stocktracker.ui.common.AssetAvatar
import com.example.stocktracker.ui.common.LoadState
import com.example.stocktracker.ui.model.DetailStableUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.model.StatRowUiModel
import com.example.stocktracker.ui.theme.MidasGreen
import com.example.stocktracker.ui.theme.MidasRed
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val livePrices by viewModel.livePrices.collectAsStateWithLifecycle()
    DetailContent(
        uiState = uiState,
        livePrices = livePrices,
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    uiState: DetailUiState,
    livePrices: Map<String, PriceDisplayUiModel>,
    onBack: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = uiState.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
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
                        imageVector = if (uiState.isInWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (uiState.isInWatchlist) "Listeden çıkar" else "Listeye ekle",
                        tint = if (uiState.isInWatchlist) MidasGreen else MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        Crossfade(
            targetState = uiState.content,
            animationSpec = tween(180),
            label = "DetailCrossfade",
        ) { content ->
            when (content) {
                LoadState.Loading -> DetailLoading()
                is LoadState.Error -> DetailError(
                    message = content.message,
                    onRetry = { onEvent(DetailEvent.Retry) },
                )
                is LoadState.Success -> DetailBody(
                    symbol = uiState.symbol,
                    content = content.data,
                    price = livePrices[uiState.id] ?: PriceDisplayUiModel.Loading,
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    symbol: String,
    content: DetailStableUiModel,
    price: PriceDisplayUiModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetAvatar(symbol = symbol, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content.pairLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Column {
            Text("Fiyat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            AnimatedPrice(
                formattedPrice = price.formattedPrice,
                priceUsd = price.priceUsd ?: 0.0,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${price.formattedChange}  •  24s",
                style = MaterialTheme.typography.bodyMedium,
                color = if (price.isPositive) MidasGreen else MidasRed,
                fontWeight = FontWeight.SemiBold,
            )
        }

        StatGroup(title = "24 Saatlik Aralık", rows = content.rangeStats)
        StatGroup(title = "Piyasa Aktivitesi", rows = content.activityStats)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatGroup(title: String, rows: List<StatRowUiModel>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            rows.forEachIndexed { index, row ->
                StatRow(row.label, row.value)
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun DetailError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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

@Preview(name = "Detail (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DetailPreview() {
    StockTrackerTheme(darkTheme = true) {
        DetailContent(
            uiState = DetailUiState(
                id = "BTCUSDT",
                symbol = "BTC",
                name = "Bitcoin",
                content = LoadState.Success(
                    DetailStableUiModel(
                        pairLabel = "BTC/USDT",
                        rangeStats = listOf(
                            StatRowUiModel("En Yüksek", "\$68,120.00"),
                            StatRowUiModel("En Düşük", "\$65,870.00"),
                            StatRowUiModel("Ortalama (VWAP)", "\$67,180.00"),
                        ),
                        activityStats = listOf(
                            StatRowUiModel("24s Hacim", "\$42.00B"),
                            StatRowUiModel("Çift", "BTC/USDT"),
                        ),
                    ),
                ),
                isInWatchlist = true,
            ),
            livePrices = mapOf(
                "BTCUSDT" to PriceDisplayUiModel(
                    formattedPrice = "\$67,400.10",
                    formattedChange = "%2,62",
                    isPositive = true,
                    priceUsd = 67_400.10,
                ),
            ),
            onBack = {},
            onEvent = {},
        )
    }
}
