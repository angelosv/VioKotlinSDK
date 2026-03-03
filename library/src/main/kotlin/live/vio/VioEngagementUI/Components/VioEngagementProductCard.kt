package live.vio.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import live.vio.sdk.domain.models.ProductDto

/**
 * Reusable Compose component for displaying a product card.
 * 
 * @deprecated Use VioEngagementProductCard instead, which uses VioEngagementCardBase
 * @param productId The product ID to display
 * @param onProductClick Callback when user clicks the product
 */
@Deprecated("Use VioEngagementProductCard instead", ReplaceWith("VioEngagementProductCard(product, sponsorLogoUrl, modifier, onAddToCart, onDismiss)"))
@Composable
fun VioEngagementProductCard(
    product: ProductDto,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onAddToCart: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = { onDismiss?.invoke() }
    ) {
        androidx.compose.material3.Text(
            text = product.title,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
