package live.vio.VioEngagementUI.Components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import live.vio.sdk.domain.models.ProductDto
import live.vio.VioDesignSystem.SponsorAssets
import live.vio.VioDesignSystem.Components.SponsorAvatar
import live.vio.VioCore.models.SponsorAssets as CoreAssets

/**
 * Reusable Compose component for displaying a product card.
 * 
 * @deprecated Use VioEngagementProductCard instead, which uses VioEngagementCardBase
 * @param productId The product ID to display
 * @param onProductClick Callback when user clicks the product
 */
@Deprecated("Use VioEngagementProductCard instead", ReplaceWith("VioEngagementProductCard(product, sponsor, modifier, onAddToCart, onDismiss)"))
@Composable
fun VioEngagementProductCard(
    product: ProductDto,
    sponsor: SponsorAssets? = CoreAssets.current,
    modifier: Modifier = Modifier,
    onAddToCart: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    VioEngagementCardBase(
        modifier = modifier,
        onDismiss = { onDismiss?.invoke() }
    ) {
        // Branding Header
        sponsor?.let {
            SponsorAvatar(sponsor = it)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        }
        androidx.compose.material3.Text(
            text = product.title,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
