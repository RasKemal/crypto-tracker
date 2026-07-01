package com.example.stocktracker.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stocktracker.R
import com.example.stocktracker.ui.model.PriceDisplayUiModel
import com.example.stocktracker.ui.theme.CryptoGreen
import com.example.stocktracker.ui.theme.CryptoRed

@Composable
fun PriceColumn(
    livePrices: State<Map<String, PriceDisplayUiModel>>,
    assetId: String,
    modifier: Modifier = Modifier,
) {
    val price = livePrices.value[assetId] ?: PriceDisplayUiModel.Loading
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        when {
            price.isLoading -> {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(modifier = Modifier.height(18.dp))
            }
            price.priceLoadFailed -> {
                Text(
                    text = price.formattedPrice,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.error_price_load),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                )
            }
            else -> {
                Text(
                    text = price.formattedPrice,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                ChangeLabel(price.formattedChange, price.isPositive)
            }
        }
    }
}

@Composable
private fun ChangeLabel(formatted: String, isPositive: Boolean) {
    Text(
        text = formatted,
        style = MaterialTheme.typography.titleSmall,
        color = if (isPositive) CryptoGreen else CryptoRed,
        fontWeight = FontWeight.SemiBold,
    )
}
