package com.reachu.viaplaydemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.reachu.viaplaydemo.ui.model.Category
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) ViaplayTheme.Colors.surface else ViaplayTheme.Colors.surface.copy(alpha = 0.6f)
    Text(
        text = category.name,
        color = if (isSelected) ViaplayTheme.Colors.textPrimary else ViaplayTheme.Colors.textSecondary,
        style = ViaplayTheme.Typography.body,
        modifier = modifier
            .clip(RoundedCornerShape(ViaplayTheme.CornerRadius.medium))
            .background(bg)
            .clickable { onClick() }
            .padding(
                horizontal = ViaplayTheme.Spacing.lg,
                vertical = ViaplayTheme.Spacing.sm,
            ),
    )
}
