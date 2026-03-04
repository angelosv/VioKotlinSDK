package live.vio.VioEngagementUI.Components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import live.vio.VioCore.managers.CampaignManager
import live.vio.VioDesignSystem.Tokens.VioColors
import live.vio.VioDesignSystem.Tokens.VioSpacing
import live.vio.VioUI.Components.compose.utils.toVioColor
import live.vio.sdk.domain.models.ProductDto

/**
 * Reusable Compose component for displaying multiple products in an overlay/modal.
 * 
 * @param products The products to display
 * @param isVisible Whether the overlay is visible
 * @param onDismiss Callback when user dismisses the overlay
 * @param onAddToCart Callback when user adds product to cart
 */
@Composable
fun VioEngagementProductOverlay(
    products: List<ProductDto>,
    isVisible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onAddToCart: ((product: ProductDto) -> Unit)? = null
) {
    val currentCampaign by CampaignManager.shared.currentCampaign.collectAsState()
    val sponsor = currentCampaign?.sponsor
    val sponsorLogoUrl = sponsor?.logoUrl
    val sponsorAvatarUrl = sponsor?.avatarUrl

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VioColors.background.toVioColor().copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with Sponsor Avatar
                if (!sponsorAvatarUrl.isNullOrBlank()) {
                    SponsorHeader(avatarUrl = sponsorAvatarUrl)
                }

                VioEngagementProductGridCard(
                    products = products,
                    sponsorLogoUrl = sponsorLogoUrl,
                    onAddToCart = { product -> onAddToCart?.invoke(product) },
                    onDismiss = { onDismiss?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun SponsorHeader(avatarUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VioSpacing.lg.dp)
            .padding(bottom = VioSpacing.md.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(avatarUrl),
            contentDescription = "Sponsor Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentScale = ContentScale.Crop
        )
    }
}
