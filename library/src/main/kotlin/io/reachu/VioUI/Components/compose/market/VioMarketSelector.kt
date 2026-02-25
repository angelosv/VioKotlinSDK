package io.reachu.VioUI.Components.compose.market

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioUI.Components.VioMarketSelector
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Market
import io.reachu.VioUI.Components.compose.utils.toVioColor

private fun String.toColor(): Color = toVioColor()

@Composable
fun VioMarketSelector(
    cartManager: CartManager,
    modifier: Modifier = Modifier,
    onSelected: (Market) -> Unit = {},
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
) {
    val controller = remember(cartManager) { VioMarketSelector(cartManager) }
    val isLoading by controller.isLoading.collectAsState(initial = true)
    val chips by controller.chips.collectAsState(initial = emptyList())

    LaunchedEffect(controller) { controller.load() }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isLoading) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
            }
        } else if (chips.isEmpty()) {
            Text("No markets available", color = MaterialTheme.colorScheme.error)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(chips, key = { it.id }) { chip ->
                    MarketChip(
                        name = chip.name,
                        code = chip.code,
                        currencyCode = chip.currencyCode,
                        currencySymbol = chip.currencySymbol,
                        flagUrl = chip.flagURL,
                        selected = chip.isSelected,
                        imageLoader = imageLoader,
                    ) {
                        controller.select(chip.code)
                        cartManager.markets.firstOrNull { it.code == chip.code }?.let(onSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketChip(
    name: String,
    code: String,
    currencyCode: String,
    currencySymbol: String,
    flagUrl: String?,
    selected: Boolean,
    imageLoader: VioImageLoader,
    onClick: () -> Unit,
) {
    val bg = if (selected) VioColors.primary.toColor() else VioColors.surfaceSecondary.toColor()
    val textPrimary = if (selected) Color.White else VioColors.textPrimary.toColor()
    val textSecondary = if (selected) Color.White.copy(alpha = 0.9f) else VioColors.textSecondary.toColor()
    val border = if (selected) Color.Transparent else VioColors.border.toColor()
    val shape = RoundedCornerShape(12.dp)

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        border = BorderStroke(1.dp, border),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!flagUrl.isNullOrBlank()) {
                VioImage(
                    url = flagUrl,
                    contentDescription = "flag-$code",
                    modifier = Modifier.size(width = 24.dp, height = 16.dp),
                    imageLoader = imageLoader,
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 24.dp, height = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(code.uppercase(), color = VioColors.textSecondary.toColor(), style = MaterialTheme.typography.labelSmall)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = textPrimary, style = MaterialTheme.typography.bodyMedium)
                Text("$currencySymbol • $currencyCode", color = textSecondary, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.weight(1f))

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "selected",
                        tint = VioColors.primary.toColor(),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
