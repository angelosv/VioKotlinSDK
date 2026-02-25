package com.reachu.tv2demo.ui.components

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
import com.reachu.tv2demo.ui.theme.TV2Theme

@Composable
fun TV2OfferBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(TV2Theme.Colors.primary, TV2Theme.Colors.secondary),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(TV2Theme.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Ukens TV2+ Sports", style = TV2Theme.Typography.caption, color = TV2Theme.Colors.textPrimary)
        Text("Live casting + shopping", style = TV2Theme.Typography.title, color = TV2Theme.Colors.textPrimary)
        Text(
            "Direkte kjøp av produkter mens du ser Barça vs PSG",
            style = TV2Theme.Typography.body,
            color = TV2Theme.Colors.textPrimary,
        )
        Spacer(Modifier.height(TV2Theme.Spacing.sm))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(TV2Theme.Colors.surface.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Se nå", color = TV2Theme.Colors.textPrimary)
        }
    }
}
