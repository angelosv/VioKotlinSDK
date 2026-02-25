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
fun ReachuEngagementProductGridCard(
    productId: String,
    onProductClick: ((productId: String) -> Unit)? = null
) {
    // Deprecated: Use VioEngagementProductGridCard instead
    // This stub is kept for backward compatibility
}
