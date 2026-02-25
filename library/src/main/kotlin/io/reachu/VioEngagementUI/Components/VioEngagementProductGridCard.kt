package io.reachu.VioEngagementUI.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.sdk.domain.models.ProductDto

/**
 * Componente Compose que muestra múltiples productos en formato grid con scroll horizontal.
 * Usa VioEngagementCardBase como contenedor y VioEngagementProductCard para cada producto.
 *
 * @param products Lista de productos a mostrar
 * @param sponsorLogoUrl URL del logo del sponsor (opcional)
 * @param modifier Modifier opcional para el contenedor
 * @param onProductClick Callback al hacer click en un producto
 * @param onAddToCart Callback al agregar un producto al carrito
 * @param onDismiss Callback para cerrar la card
 */
@Composable
fun VioEngagementProductGridCard(
    products: List<ProductDto>,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onProductClick: (ProductDto) -> Unit,
    onAddToCart: (ProductDto) -> Unit,
    onDismiss: () -> Unit,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = onDismiss,
    ) {
        if (products.isEmpty()) {
            Text(
                text = "No hay productos disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = VioColors.textSecondary.toVioColor(),
            )
            return@VioEngagementCardBase
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(products, key = { it.id }) { product ->
                ProductGridItem(
                    product = product,
                    onProductClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) },
                )
            }
        }
    }
}

@Composable
private fun ProductGridItem(
    product: ProductDto,
    onProductClick: () -> Unit,
    onAddToCart: () -> Unit,
) {
    val primaryImage = product.images.firstOrNull()?.url
    val price = product.price.amountInclTaxes ?: product.price.amount
    val currencyCode = product.price.currencyCode.ifBlank { "" }
    val formattedPrice = if (currencyCode.length > 1) {
        "$currencyCode ${"%.2f".format(price)}"
    } else {
        "$currencyCode${"%.2f".format(price)}"
    }

    Column(
        modifier = Modifier
            .size(width = 160.dp, height = 220.dp)
            .clip(RoundedCornerShape(VioBorderRadius.medium.dp))
            .background(VioColors.surface.toVioColor())
            .clickable { onProductClick() }
            .padding(VioSpacing.sm.dp),
        verticalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp),
    ) {
        // Imagen del producto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(VioBorderRadius.small.dp))
                .background(VioColors.backgroundMuted.toVioColor()),
        ) {
            VioImage(
                url = primaryImage,
                contentDescription = product.title,
                modifier = Modifier.fillMaxWidth(),
                imageLoader = VioImageLoaderDefaults.current,
            )
        }

        // Título
        Text(
            text = product.title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = VioColors.textPrimary.toVioColor(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Precio
        Text(
            text = formattedPrice,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = VioColors.primary.toVioColor(),
        )

        Spacer(modifier = Modifier.height(VioSpacing.xs.dp))

        // Botón compacto de agregar al carrito
        Button(
            onClick = onAddToCart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioColors.primary.toVioColor(),
                contentColor = VioColors.textOnPrimary.toVioColor(),
            ),
            shape = RoundedCornerShape(VioBorderRadius.small.dp),
        ) {
            Text(
                text = "Legg i kurv",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
