package com.example.stocktracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stocktracker.ui.theme.StockTrackerTheme
import com.example.stocktracker.ui.theme.avatarPalette
import kotlin.math.absoluteValue

// Color is derived deterministically from the symbol hash so the same symbol
// always shows the same color across recompositions and app restarts.
@Composable
fun StockAvatar(
    symbol: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val letter = remember(symbol) { symbol.firstOrNull { it.isLetter() }?.uppercase() ?: "?" }
    val background = remember(symbol) { avatarPalette[symbol.hashCode().absoluteValue % avatarPalette.size] }

    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = (size.value * 0.40f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StockAvatarPreview() {
    StockTrackerTheme(darkTheme = true) {
        Box {
            listOf("AAPL", "THYAO", "BTCUSDT", "LVWR", "EOSE").forEach {
                StockAvatar(symbol = it, modifier = Modifier.size(46.dp))
            }
        }
    }
}
