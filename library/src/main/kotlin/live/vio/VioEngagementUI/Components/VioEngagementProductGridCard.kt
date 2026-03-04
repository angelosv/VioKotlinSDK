package live.vio.VioEngagementUI.Components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import live.vio.VioDesignSystem.Tokens.VioBorderRadius
import live.vio.VioDesignSystem.Tokens.VioColors
import live.vio.VioDesignSystem.Tokens.VioSpacing
import live.vio.VioUI.Components.compose.utils.toVioColor
import live.vio.sdk.domain.models.ProductDto

/**
 * Reusable Compose component for displaying a product in a grid layout within an engagement.
 * 
 * @param products The list of products to display
 * @param sponsorLogoUrl The sponsor's logo URL
 * @param modifier The modifier to be applied to the card
 * @param onProductClick Callback when user clicks the product
 * @param onAddToCart Callback when user adds product to cart
 * @param onDismiss Callback when user dismisses the card
 */
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            Text(
                text = "Produkter",
                style = MaterialTheme.typography.titleMedium,
                color = VioColors.textPrimary.toVioColor(),
                modifier = Modifier.padding(bottom = VioSpacing.md.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(products) { product ->
                    ProductGridItem(
                        product = product,
                        onClick = { onProductClick?.invoke(product) },
                        onAddToCart = { onAddToCart?.invoke(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductGridItem(
    product: ProductDto,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(VioBorderRadius.medium.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(VioSpacing.sm.dp)
        ) {
            // Product Image
            val imageUrl = product.images.firstOrNull()?.url
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(VioBorderRadius.small.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

            // Product Title
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = VioColors.textPrimary.toVioColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 40.dp)
            )

            Spacer(modifier = Modifier.height(VioSpacing.xs.dp))

            // Product Price
            val priceText = "${product.price.amount.toInt()} NOK"
            Text(
                text = priceText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = VioColors.textSecondary.toVioColor()
            )

            Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

            // CTA Button
            Button(
                onClick = onAddToCart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(VioBorderRadius.small.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VioColors.primary.toVioColor(),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(
                    text = "KJØP",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
