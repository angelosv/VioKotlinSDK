package com.reachu.demoapp.viaplay.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.reachu.demoapp.viaplay.models.CastingProductEvent
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Components.CachedAsyncImage

@Composable
fun CastingProductModal(
    productEvents: List<CastingProductEvent>,
    initialIndex: Int = 0,
    onDismiss: () -> Void
) {
    val campaignManager = remember { CampaignManager.shared }
    val currentCampaign by campaignManager.currentCampaign.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialIndex) { productEvents.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campaign Logo
                CachedAsyncImage(
                    url = currentCampaign?.campaignLogo,
                    contentDescription = "Campaign Logo",
                    modifier = Modifier.height(30.dp),
                    placeholder = { Box(Modifier.size(30.dp)) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Close Button
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Horizontal Pager for swiping between products
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ProductCheckoutContent(
                    productEvent = productEvents[page],
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
fun ProductCheckoutContent(
    productEvent: CastingProductEvent,
    onDismiss: () -> Void
) {
    val urlToLoad = productEvent.castingCheckoutUrl ?: productEvent.castingProductUrl

    // Content Area - WebView or Error
    if (urlToLoad != null) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(urlToLoad)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Error view when no URL is available
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Checkout URL ikke tilgjengelig",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kontakt Elkjøp for å fullføre kjøpet",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
