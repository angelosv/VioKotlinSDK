package io.reachu.VioEngagementUI.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.reachu.sdk.domain.models.ProductDto

/**
 * Reusable Compose component for displaying a product card.
 * 
 * @deprecated Use VioEngagementProductCard instead, which uses VioEngagementCardBase
 * @param productId The product ID to display
 * @param onProductClick Callback when user clicks the product
 */
@Deprecated("Use VioEngagementProductCard instead", ReplaceWith("VioEngagementProductCard(product, sponsorLogoUrl, modifier, onAddToCart, onDismiss)"))
@Composable
fun ReachuEngagementProductCard(
    productId: String,
    onProductClick: ((productId: String) -> Unit)? = null
) {
    // Deprecated: Use VioEngagementProductCard instead
    // This stub is kept for backward compatibility
}
