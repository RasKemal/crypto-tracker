package com.example.stocktracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stocktracker.domain.model.MarketFilter
import com.example.stocktracker.ui.theme.StockTrackerTheme

@Composable
fun MarketFilterChips(
    selectedFilter: MarketFilter,
    onFilterSelected: (MarketFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = remember { MarketFilter.entries }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(filters, key = { it.name }) { filter ->
            MarketChip(
                label = filter.label,
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun MarketChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val chipShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(chipShape)
            .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MarketFilterChipsPreview() {
    StockTrackerTheme(darkTheme = true) {
        MarketFilterChips(
            selectedFilter = MarketFilter.ABD,
            onFilterSelected = {},
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
