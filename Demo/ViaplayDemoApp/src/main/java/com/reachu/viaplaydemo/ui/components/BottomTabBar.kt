package com.reachu.viaplaydemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachu.viaplaydemo.ui.model.TabItem
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun BottomTabBar(
    selected: TabItem,
    onTabSelected: (TabItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        TabItem.HOME to Icons.Default.Home,
        TabItem.SPORT to Icons.Default.Sports,
        TabItem.STREAMS to Icons.Default.LiveTv,
        TabItem.STORE to Icons.Default.ShoppingCart,
        TabItem.PROFILE to Icons.Default.Person,
    )
    Row(
        modifier = modifier
            .background(ViaplayTheme.Colors.surface)
            .padding(vertical = ViaplayTheme.Spacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, icon) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tab.label,
                    tint = if (tab == selected) ViaplayTheme.Colors.primary else ViaplayTheme.Colors.textSecondary,
                )
                Text(
                    tab.label,
                    style = ViaplayTheme.Typography.small,
                    color = if (tab == selected) ViaplayTheme.Colors.primary else ViaplayTheme.Colors.textSecondary,
                )
            }
        }
    }
}
