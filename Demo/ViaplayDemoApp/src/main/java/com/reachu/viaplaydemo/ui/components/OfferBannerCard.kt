package com.reachu.viaplaydemo.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun TV2OfferBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(ViaplayTheme.Colors.primary, ViaplayTheme.Colors.secondary),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(ViaplayTheme.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Ukens TV2+ Sports", style = ViaplayTheme.Typography.caption, color = ViaplayTheme.Colors.textPrimary)
        Text("Live casting + shopping", style = ViaplayTheme.Typography.title, color = ViaplayTheme.Colors.textPrimary)
        Text(
            "Direkte kjøp av produkter mens du ser Barça vs PSG",
            style = ViaplayTheme.Typography.body,
            color = ViaplayTheme.Colors.textPrimary,
        )
        Spacer(Modifier.height(ViaplayTheme.Spacing.sm))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ViaplayTheme.Colors.surface.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Se nå", color = ViaplayTheme.Colors.textPrimary)
        }
    }
}
