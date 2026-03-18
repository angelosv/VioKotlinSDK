package live.vio.VioEngagementUI.Components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.managers.VioCommerceService
import live.vio.VioCore.models.CommerceProduct
import live.vio.VioCore.models.SponsorAssets as CoreAssets
import live.vio.VioDesignSystem.Components.SponsorAvatar
import live.vio.VioDesignSystem.Tokens.VioColors
import live.vio.VioDesignSystem.Tokens.VioSpacing
import live.vio.VioUI.Components.VPaymentSheet
import live.vio.VioDesignSystem.Components.CachedAsyncImage
import live.vio.VioUI.Components.compose.utils.toVioColor

/**
 * A highly visual engagement overlay for a single product.
 * Features a slide-up animation, 15s auto-dismiss, and Google Pay integration.
 * Mirrors the VProductDetailOverlay from the Swift SDK.
 */
@Composable
fun VioEngagementProductDetailOverlay(
    productId: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (CommerceProduct) -> Unit = {}
) {
    var product by remember(productId) { mutableStateOf<CommerceProduct?>(null) }
    var isLoading by remember(productId) { mutableStateOf(true) }
    val configState by VioConfiguration.shared.state.collectAsState()
    val hasGooglePay = configState.campaign.hasGooglePay == true
    val sponsor = CoreAssets.current

    // Fetch product details
    LaunchedEffect(productId) {
        isLoading = true
        product = VioCommerceService.fetchProduct(productId)
        isLoading = false
    }

    // Auto-dismiss after 15 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(15000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        // Overlay Root with dim background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onDismiss() }, // Dismiss on tap outside
            contentAlignment = Alignment.BottomCenter
        ) {
            // Main Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(VioSpacing.md.dp)
                    .clickable(enabled = false) { }, // Prevent clicks on card from dismissing
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VioColors.background.toVioColor()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(VioSpacing.lg.dp)
                ) {
                    // Close Button Layer
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VioColors.primary.toVioColor())
                        }
                    } else if (product != null) {
                        val currentProduct = product!!
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Product Image
                            CachedAsyncImage(
                                url = currentProduct.primaryImageUrl,
                                contentDescription = currentProduct.name,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(VioSpacing.md.dp))

                            // Product Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentProduct.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Text(
                                    text = currentProduct.formattedPrice,
                                    color = VioColors.primary.toVioColor(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Sponsor Identity
                                sponsor?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SponsorAvatar(sponsor = it, size = 24)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = it.badgeText ?: "Sponset av ${it.name}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Interaction Layer: standard CTA or Payment Sheet
                        if (hasGooglePay) {
                            VPaymentSheet(
                                onPaymentMethodSelected = {
                                    // Handle payment selection
                                    onDismiss()
                                }
                            )
                        } else {
                            Button(
                                onClick = { 
                                    onAddToCart(currentProduct)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VioColors.primary.toVioColor(),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Legg til i handlekurv",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        // Error State
                        Text(
                            text = "Kunne ikke laste produktinformasjon",
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
