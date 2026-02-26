package io.reachu.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.reachu.sdk.domain.models.ProductDto

/**
 * Reusable Compose component for displaying a product in a grid layout.
 * 
 * @deprecated Use VioEngagementProductGridCard instead, which uses VioEngagementCardBase
 * @param productId The product ID to display
 * @param onProductClick Callback when user clicks the product
 */
@Deprecated("Use VioEngagementProductGridCard instead", ReplaceWith("VioEngagementProductGridCard(products, sponsorLogoUrl, modifier, onProductClick, onAddToCart, onDismiss)"))
@Composable
fun VioEngagementProductGridCard(
    products: List<ProductDto>,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onProductClick: ((ProductDto) -> Unit)? = null,
    onAddToCart: ((ProductDto) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = { onDismiss?.invoke() }
    ) {
        // Grid implementation stub
        androidx.compose.material3.Text(
            text = "Products (${products.size})",
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
