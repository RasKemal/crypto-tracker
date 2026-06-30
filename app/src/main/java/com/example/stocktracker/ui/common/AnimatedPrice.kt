package com.example.stocktracker.ui.common

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stocktracker.ui.theme.MidasGreen
import com.example.stocktracker.ui.theme.MidasRed

@Composable
fun AnimatedPrice(
    formattedPrice: String,
    priceUsd: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    val foreground = MaterialTheme.colorScheme.onBackground
    val highlight = remember { Animatable(Color.Transparent) }

    var previousPrice by remember { mutableDoubleStateOf(priceUsd) }
    var seeded by remember { mutableStateOf(false) }

    LaunchedEffect(priceUsd) {
        if (!seeded) {
            seeded = true
            previousPrice = priceUsd
            return@LaunchedEffect
        }
        if (priceUsd == previousPrice) return@LaunchedEffect

        val direction = priceUsd.compareTo(previousPrice)
        previousPrice = priceUsd
        val flash = if (direction > 0) MidasGreen else MidasRed
        highlight.snapTo(flash.copy(alpha = 0.22f))
        highlight.animateTo(Color.Transparent, animationSpec = tween(700))
    }

    Text(
        text = formattedPrice,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(highlight.value)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        style = style,
        color = foreground,
        fontWeight = fontWeight,
    )
}
