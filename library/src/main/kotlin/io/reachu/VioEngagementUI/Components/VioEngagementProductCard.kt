package io.reachu.VioEngagementUI.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 * Componente Compose que muestra un producto en formato card con imagen, título, precio,
 * y botón de agregar al carrito, usando VioEngagementCardBase como contenedor.
 *
 * @param product Modelo de producto a mostrar
 * @param sponsorLogoUrl URL del logo del sponsor (opcional)
 * @param modifier Modifier opcional para el contenedor
 * @param onAddToCart Callback al agregar al carrito
 * @param onDismiss Callback para cerrar la card
 */
@Composable
fun VioEngagementProductCard(
    product: ProductDto,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onAddToCart: (ProductDto) -> Unit,
    onDismiss: () -> Unit,
) {
    val primaryImage = product.images.firstOrNull()?.url
    val price = product.price.amountInclTaxes ?: product.price.amount
    val currencyCode = product.price.currencyCode.ifBlank { "" }
    val formattedPrice = if (currencyCode.length > 1) {
        "$currencyCode ${"%.2f".format(price)}"
    } else {
        "$currencyCode${"%.2f".format(price)}"
    }

    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(VioBorderRadius.medium.dp))
                    .background(VioColors.backgroundMuted.toVioColor()),
                contentAlignment = Alignment.Center,
            ) {
                VioImage(
                    url = primaryImage,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    imageLoader = VioImageLoaderDefaults.current,
                )
            }

            // Información del producto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = VioColors.textPrimary.toVioColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = VioColors.textSecondary.toVioColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VioColors.primary.toVioColor(),
                )

                Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

                Button(
                    onClick = { onAddToCart(product) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioColors.primary.toVioColor(),
                        contentColor = VioColors.textOnPrimary.toVioColor(),
                    ),
                    shape = RoundedCornerShape(VioBorderRadius.medium.dp),
                ) {
                    Text(
                        text = "Legg i kurv",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}
