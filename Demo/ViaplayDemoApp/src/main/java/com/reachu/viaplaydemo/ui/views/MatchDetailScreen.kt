package com.reachu.viaplaydemo.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.reachu.viaplaydemo.casting.CastingManager
import com.reachu.viaplaydemo.ui.components.BottomTabBar
import com.reachu.viaplaydemo.ui.components.OfferBannerView
import com.reachu.viaplaydemo.ui.components.TV2VideoPlayer
import com.reachu.viaplaydemo.ui.model.Match
import com.reachu.viaplaydemo.ui.model.MatchMocks
import com.reachu.viaplaydemo.ui.model.TabItem
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator
import io.reachu.VioUI.Components.compose.product.VioProductSlider
import io.reachu.VioUI.Managers.CartManager
import io.reachu.sdk.core.VioSdkClient
import java.net.URL

@Composable
fun MatchDetailScreen(
    match: Match,
    cartManager: CartManager,
    onBack: () -> Unit,
) {
    val castingManager = remember { CastingManager.shared }
    val castingState by castingManager.state.collectAsState()
    val configState = VioConfiguration.shared.state.value
    val sdkClient = remember(configState.apiKey, configState.environment) {
        val apiKey = configState.apiKey.ifBlank { "DEMO_KEY" }
        VioSdkClient(URL(configState.environment.graphQLUrl), apiKey)
    }
    var showVideo by rememberSaveable { mutableStateOf(false) }
    var showCastSheet by rememberSaveable { mutableStateOf(false) }
    var showCasting by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(TabItem.HOME) }

    Box(modifier = Modifier.fillMaxSize().background(ViaplayTheme.Colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HeroSection(
                match = match,
                isCasting = castingState.isCasting,
                onBack = onBack,
                onPlay = { showVideo = true },
                onCast = { showCastSheet = true },
            )
            
            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Button
                androidx.compose.material3.Button(
                    onClick = { showVideo = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFF5146B)),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
                
                // Casting Demo Button
                androidx.compose.material3.Button(
                    onClick = { showCasting = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFF5146B)),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Tv, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Casting Demo", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Previous Button
                    androidx.compose.material3.Button(
                        onClick = { },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2D36)),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        Text("⏪", color = Color.White)
                    }
                    
                    // Share Button
                    androidx.compose.material3.Button(
                        onClick = { },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2D36)),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        Text("Share", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                }
            }
            
            MatchDetails(match = match)
            Spacer(modifier = Modifier.height(16.dp))
            VioProductSlider(
                cartManager = cartManager,
                title = "Produkter",
                layout = io.reachu.VioUI.Components.VioProductSliderLayout.CARDS,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OfferBannerView()
            Spacer(modifier = Modifier.height(120.dp))
        }
        BottomTabBar(
            selected = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        VioFloatingCartIndicator(cartManager = cartManager, modifier = Modifier.fillMaxSize(), customPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp, end = 16.dp))
    }

    if (showVideo) {
        Dialog(
            onDismissRequest = { showVideo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            TV2VideoPlayer(
                match = match,
                cartManager = cartManager,
                sdkClient = sdkClient,
                onDismiss = { showVideo = false },
            )
        }
    }
    if (showCastSheet) {
        CastDeviceSelectionSheet(
            devices = castingManager.devices,
            onDismiss = { showCastSheet = false },
            onDeviceSelected = {
                castingManager.startCasting(it)
                showCastSheet = false
                showCasting = true
            },
        )
    }
    if (showCasting) {
        Dialog(
            onDismissRequest = { showCasting = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            CastingActiveView(
                match = match,
                cartManager = cartManager,
                sdkClient = sdkClient,
                onClose = { showCasting = false },
            )
        }
    }
}

@Preview
@Composable
private fun MatchDetailPreview() {
    MatchDetailScreen(match = MatchMocks.realMadridVsBarcelona, cartManager = CartManager(), onBack = {})
}

@Composable
private fun HeroSection(
    match: Match,
    isCasting: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onCast: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        AsyncImage(
            model = com.reachu.viaplaydemo.utils.LogoResolver.resolveImage(match.backgroundImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCast) {
                Icon(
                    imageVector = if (isCasting) Icons.Filled.Tv else Icons.Filled.Cast,
                    contentDescription = null,
                    tint = if (isCasting) ViaplayTheme.Colors.primary else Color.White,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(match.title, style = ViaplayTheme.Typography.largeTitle, color = Color.White)
            Text(match.subtitle, style = ViaplayTheme.Typography.body, color = Color.White.copy(alpha = 0.8f))
            androidx.compose.material3.Button(onClick = onPlay, modifier = Modifier.padding(top = 8.dp)) {
                Text("Spill av")
            }
        }
    }
}

@Composable
private fun MatchDetails(match: Match) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamBadge(match.homeTeam.name, match.homeTeam.logo)
            Text("VS", style = ViaplayTheme.Typography.headline, color = ViaplayTheme.Colors.textPrimary)
            TeamBadge(match.awayTeam.name, match.awayTeam.logo)
        }
        Text(
            text = "Fra ${match.venue}",
            style = ViaplayTheme.Typography.body,
            color = ViaplayTheme.Colors.textPrimary,
        )
        Text(
            text = "Kommentator: ${match.commentator ?: "TBA"}",
            style = ViaplayTheme.Typography.body,
            color = ViaplayTheme.Colors.textSecondary,
        )
        Text(
            text = match.availability.title,
            style = ViaplayTheme.Typography.title,
            color = ViaplayTheme.Colors.textPrimary,
        )
        Text(
            text = match.availability.description,
            style = ViaplayTheme.Typography.body,
            color = ViaplayTheme.Colors.textSecondary,
        )
    }
}

@Composable
private fun TeamBadge(name: String, logo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = com.reachu.viaplaydemo.utils.LogoResolver.resolveLogo(logo),
            contentDescription = name,
            modifier = Modifier.size(64.dp),
        )
        Text(name, style = ViaplayTheme.Typography.small, color = ViaplayTheme.Colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
    }
}
