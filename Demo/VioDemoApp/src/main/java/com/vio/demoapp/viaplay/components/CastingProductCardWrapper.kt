package com.vio.demoapp.viaplay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vio.demoapp.viaplay.models.CastingProductEvent
import live.vio.VioEngagementUI.Components.VioEngagementProductGridCard
import live.vio.VioUI.Managers.CartManager
import live.vio.VioUI.Managers.toDto
import live.vio.sdk.domain.models.ProductDto

/**
 * Wrapper for VioEngagementProductGridCard specifically for the casting demo.
 * Handles product conversion and state management.
 */
@Composable
fun CastingProductCardWrapper(
    productEvent: CastingProductEvent,
    cartManager: CartManager,
    onProductClick: (ProductDto) -> Unit,
    onAddToCart: (ProductDto) -> Unit,
    onDismiss: () -> Unit
) {
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(productEvent) {
        isLoading = true
        // For the demo, we filter from the already loaded products in CartManager
        // If they are not loaded, in a real scenario we would call an API
        products = cartManager.products
            .filter { it.id.toString() in productEvent.allProductIds }
            .map { it.toDto() }
        isLoading = false
    }

    VioEngagementProductGridCard(
        products = products,
        sponsorLogoUrl = productEvent.metadata?.get("sponsorLogoUrl"),
        onProductClick = onProductClick,
        onAddToCart = onAddToCart,
        onDismiss = onDismiss
    )
}
