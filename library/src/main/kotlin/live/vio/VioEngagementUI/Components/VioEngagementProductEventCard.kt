package live.vio.VioEngagementUI.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import live.vio.VioUI.Services.ProductService
import live.vio.sdk.domain.models.ProductDto

@Composable
fun VioEngagementProductEventCard(
    productId: String,
    currency: String,
    country: String,
    onAddToCart: (ProductDto) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var product by remember(productId) { mutableStateOf<ProductDto?>(null) }
    var isLoading by remember(productId) { mutableStateOf(true) }

    LaunchedEffect(productId, currency, country) {
        isLoading = true
        product = runCatching {
            ProductService.loadProductDto(
                productId = productId,
                currency = currency,
                country = country,
            )
        }.getOrNull()
        isLoading = false
        if (product == null) {
            onDismiss()
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
    } else {
        val currentProduct = product
        if (currentProduct != null) {
            VioEngagementProductCard(
                product = currentProduct,
                onAddToCart = { onAddToCart(currentProduct) },
                onDismiss = onDismiss,
            )
        }
    }
}
