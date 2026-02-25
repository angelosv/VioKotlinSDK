package com.reachu.tv2demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.models.ComponentManager
import io.reachu.VioCore.models.OfferBannerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OfferBannerDemo(
    componentManager: ComponentManager = remember { ComponentManager.shared },
) {
    var showDemo by remember { mutableStateOf(false) }
    val bannerConfig by componentManager.activeBanner.collectAsState()

    LaunchedEffect(componentManager) {
        componentManager.connect()
    }
    DisposableEffect(componentManager) {
        onDispose { componentManager.disconnect() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Offer Banner Demo", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { showDemo = !showDemo }) {
            Text(if (showDemo) "Skjul demo" else "Vis demo banner")
        }
        Spacer(modifier = Modifier.height(12.dp))

        val currentBanner = bannerConfig
        when {
            currentBanner != null -> {
                OfferBannerView(
                    title = currentBanner.title,
                    subtitle = currentBanner.subtitle,
                    backgroundImageUrl = currentBanner.backgroundImageUrl,
                )
            }

            showDemo -> {
                OfferBannerView(
                    title = "Ukens tilbud",
                    subtitle = "Se denne ukes beste tilbud",
                    backgroundImageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=1200",
                )
            }

            else -> Text("No active banner", color = MaterialTheme.colorScheme.outline)
        }
    }
}
