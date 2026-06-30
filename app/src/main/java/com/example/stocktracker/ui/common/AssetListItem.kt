package com.example.stocktracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stocktracker.ui.model.AssetUiModel
import com.example.stocktracker.ui.model.WatchlistItemUiModel
import com.example.stocktracker.ui.theme.MidasGreen
import com.example.stocktracker.ui.theme.MidasRed
import com.example.stocktracker.ui.theme.MidasSecondaryText
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun AssetListItem(
    asset: AssetUiModel,
    onClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
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
            Column(horizontalAlignment = Alignment.End) {
                AnimatedPrice(
                    formattedPrice = asset.formattedPrice,
                    priceUsd = asset.livePriceUsd,
                )
                ChangeLabel(asset.formattedChange, asset.isPositive)
            }
            IconButton(onClick = onWatchlistToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (asset.isInWatchlist) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (asset.isInWatchlist) "Listeden çıkar" else "Listeye ekle",
                    tint = if (asset.isInWatchlist) MidasGreen else MidasSecondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
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
fun WatchlistListItem(
    item: WatchlistItemUiModel,
    onClick: () -> Unit,
    onRemove: () -> Unit,
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
            RankLabel(rank = item.rank)
            Spacer(Modifier.width(10.dp))
            AssetAvatar(symbol = item.symbol)
            Spacer(Modifier.width(12.dp))
            SymbolDescription(item.symbol, item.name, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                AnimatedPrice(
                    formattedPrice = item.formattedPrice,
                    priceUsd = item.livePriceUsd,
                )
                ChangeLabel(item.formattedChange, item.isPositive)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircleOutline,
                    contentDescription = "İzleme listesinden çıkar",
                    tint = MidasSecondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
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

@Composable
private fun ChangeLabel(formatted: String, isPositive: Boolean) {
    Text(
        text = formatted,
        style = MaterialTheme.typography.titleSmall,
        color = if (isPositive) MidasGreen else MidasRed,
        fontWeight = FontWeight.SemiBold,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AssetListItemPreview() {
    StockTrackerTheme(darkTheme = true) {
        Column {
            AssetListItem(
                asset = AssetUiModel("bitcoin", "BTC", "Bitcoin", 1, 67320.45, "\$67,320.45", "%2,45", true, false),
                onClick = {}, onWatchlistToggle = {},
            )
            AssetListItem(
                asset = AssetUiModel("solana", "SOL", "Solana", 2, 147.85, "\$147.85", "%3,78", true, true),
                onClick = {}, onWatchlistToggle = {},
            )
            AssetListItem(
                asset = AssetUiModel("xrp", "XRP", "XRP", 3, 0.5512, "\$0.5512", "-%1,20", false, false),
                onClick = {}, onWatchlistToggle = {},
            )
        }
    }
}
