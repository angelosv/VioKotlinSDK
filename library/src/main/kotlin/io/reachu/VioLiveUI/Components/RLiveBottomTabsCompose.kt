package io.reachu.liveui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.models.Product
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.LiveShowCartManagerProvider
import io.reachu.liveshow.models.LiveProduct

@Composable
fun VioLiveBottomTabs(
    products: List<LiveProduct>,
    manager: LiveShowManager = LiveShowManager.shared,
    modifier: Modifier = Modifier,
    onProductSelected: (Product) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(TabType.CHAT) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.75f)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            when (selected) {
                TabType.CHAT -> VioLiveChatComponent(
                    controller = remember { VioLiveChatComponentController() },
                    modifier = Modifier.fillMaxWidth(),
                )
                TabType.PRODUCTS -> VioLiveProductsComponent(
                    products = products,
                    manager = manager,
                    cartManager = LiveShowCartManagerProvider.default,
                    onProductSelected = onProductSelected,
                )
            }
        }
        TabBar(
            expanded = expanded,
            selected = selected,
            onToggle = { expanded = !expanded },
            onSelect = { selected = it },
        )
    }
}

@Composable
private fun TabBar(
    expanded: Boolean,
    selected: TabType,
    onToggle: () -> Unit,
    onSelect: (TabType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowDropUp,
                contentDescription = null,
                tint = Color.White,
            )
        }
        TabType.values().forEach { tab ->
            Surface(
                tonalElevation = if (selected == tab) 8.dp else 0.dp,
                color = if (selected == tab) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                onClick = { onSelect(tab) },
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = if (selected == tab) Color.White else Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected == tab) Color.White else Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

enum class TabType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CHAT("Chat", Icons.Filled.Chat),
    PRODUCTS("Shop", Icons.Filled.ShoppingBag);
}
