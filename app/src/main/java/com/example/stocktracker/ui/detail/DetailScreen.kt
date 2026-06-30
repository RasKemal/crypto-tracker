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
import com.example.stocktracker.domain.model.CryptoAsset
import com.example.stocktracker.ui.common.AnimatedPrice
import com.example.stocktracker.ui.common.AssetAvatar
import com.example.stocktracker.ui.model.formatChangePercent
import com.example.stocktracker.ui.model.formatLargeUsd
import com.example.stocktracker.ui.model.formatUsd
import com.example.stocktracker.ui.theme.MidasGreen
import com.example.stocktracker.ui.theme.MidasRed
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailContent(uiState = uiState, onBack = onBack, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    uiState: DetailUiState,
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
            targetState = when {
                uiState.asset != null -> DetailRenderState.Loaded(uiState.asset)
                uiState.error != null -> DetailRenderState.Error(uiState.error)
                else                  -> DetailRenderState.Loading
            },
            animationSpec = tween(180),
            label = "DetailCrossfade",
        ) { state ->
            when (state) {
                is DetailRenderState.Loaded -> DetailBody(uiState = uiState, asset = state.asset)
                is DetailRenderState.Error  -> DetailError(state.message)
                DetailRenderState.Loading   -> DetailLoading()
            }
        }
    }
}

private sealed interface DetailRenderState {
    data object Loading : DetailRenderState
    data class Error(val message: String) : DetailRenderState
    data class Loaded(val asset: CryptoAsset) : DetailRenderState
}

@Composable
private fun DetailBody(uiState: DetailUiState, asset: CryptoAsset) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetAvatar(symbol = asset.symbol, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${asset.symbol}/${asset.quoteAsset}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val price = uiState.liveTick?.price ?: asset.priceUsd
        val changePct = uiState.liveTick?.changePercent24Hr ?: asset.changePercent24Hr
        Column {
            Text("Fiyat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            AnimatedPrice(
                formattedPrice = price.formatUsd(),
                priceUsd = price,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${changePct.formatChangePercent()}  •  24s",
                style = MaterialTheme.typography.bodyMedium,
                color = if (changePct >= 0) MidasGreen else MidasRed,
                fontWeight = FontWeight.SemiBold,
            )
        }

        StatGroup(
            title = "24 Saatlik Aralık",
            rows = listOf(
                "En Yüksek" to (asset.high24Hr?.formatUsd() ?: "--"),
                "En Düşük"  to (asset.low24Hr?.formatUsd() ?: "--"),
                "Ortalama (VWAP)" to (asset.vwap24Hr?.formatUsd() ?: "--"),
            ),
        )

        StatGroup(
            title = "Piyasa Aktivitesi",
            rows = listOf(
                "24s Hacim" to (asset.volumeUsd24Hr?.formatLargeUsd() ?: "--"),
                "Çift"      to "${asset.symbol}/${asset.quoteAsset}",
            ),
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatGroup(title: String, rows: List<Pair<String, String>>) {
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
            rows.forEachIndexed { index, (label, value) ->
                StatRow(label, value)
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
private fun DetailError(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Detail (Dark)", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DetailPreview() {
    StockTrackerTheme(darkTheme = true) {
        DetailContent(
            uiState = DetailUiState(
                id = "BTCUSDT", symbol = "BTC", name = "Bitcoin",
                asset = CryptoAsset(
                    id = "BTCUSDT", symbol = "BTC", name = "Bitcoin",
                    quoteAsset = "USDT",
                    priceUsd = 67_320.45, changePercent24Hr = 2.45,
                    high24Hr = 68_120.0, low24Hr = 65_870.0,
                    volumeUsd24Hr = 42_000_000_000.0,
                    vwap24Hr = 67_180.0,
                ),
                liveTick = com.example.stocktracker.domain.model.LivePrice(
                    id = "BTCUSDT", price = 67_400.10, changePercent24Hr = 2.62,
                ),
                isInWatchlist = true,
                isLoading = false,
            ),
            onBack = {},
            onEvent = {},
        )
    }
}
