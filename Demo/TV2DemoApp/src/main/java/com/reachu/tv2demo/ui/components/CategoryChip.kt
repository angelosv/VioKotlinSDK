package com.reachu.tv2demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.reachu.tv2demo.ui.model.Category
import com.reachu.tv2demo.ui.theme.TV2Theme

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) TV2Theme.Colors.surface else TV2Theme.Colors.surface.copy(alpha = 0.6f)
    Text(
        text = category.name,
        color = if (isSelected) TV2Theme.Colors.textPrimary else TV2Theme.Colors.textSecondary,
        style = TV2Theme.Typography.body,
        modifier = modifier
            .clip(RoundedCornerShape(TV2Theme.CornerRadius.medium))
            .background(bg)
            .clickable { onClick() }
            .padding(
                horizontal = TV2Theme.Spacing.lg,
                vertical = TV2Theme.Spacing.sm,
            ),
    )
}
