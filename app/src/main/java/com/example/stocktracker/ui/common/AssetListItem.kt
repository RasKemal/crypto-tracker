package com.example.stocktracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun AssetListItem(
    asset: AssetUiModel,
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    onClick: () -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankLabel(rank = asset.rank)
            Spacer(Modifier.width(10.dp))
            AssetAvatar(symbol = asset.symbol)
            Spacer(Modifier.width(12.dp))
            SymbolDescription(asset.symbol, asset.name, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            PriceColumn(
                price = livePrices.value[asset.id] ?: PriceDisplayUiModel.Loading,
            )
            trailingIcon()
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun RankLabel(rank: Int) {
    Text(
        text = rank.toString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(20.dp),
    )
}

@Composable
private fun SymbolDescription(symbol: String, name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AssetListItemPreview() {
    val livePrices = remember {
        mutableStateOf(
            mapOf(
                "BTCUSDT" to PriceDisplayUiModel("\$67,320.45", "%2,45", true, 67320.45),
                "ETHUSDT" to PriceDisplayUiModel.Loading,
            ),
        )
    }
    StockTrackerTheme(darkTheme = true) {
        Column {
            AssetListItem(
                asset = AssetUiModel("BTCUSDT", "BTC", "Bitcoin", 1, false),
                livePrices = livePrices,
                onClick = {},
                trailingIcon = {},
            )
        }
    }
}
