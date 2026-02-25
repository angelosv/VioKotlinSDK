package io.reachu.VioEngagementUI.Components

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioUI.Components.VioProductCardState
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.toVioProductCardState
import io.reachu.sdk.domain.models.ProductDto

/**
 * Overlay de producto para engagement:
 * - Se posiciona de forma adaptable (bottom en portrait, right en landscape).
 * - Muestra imagen, título, descripción y precio.
 * - Permite agregar al carrito y cerrar con drag o callback.
 */
@Composable
fun VioEngagementProductOverlay(
    product: ProductDto,
    onAddToCart: (ProductDto) -> Unit,
    onDismiss: () -> Unit,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val cardState = product.toVioProductCardState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VioColors.background.toVioColor().copy(alpha = 0.0f)),
    ) {
        val alignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = alignment,
        ) {
            VioEngagementCardBase(
                sponsorLogoUrl = sponsorLogoUrl,
                onDismiss = onDismiss,
            ) {
                ProductContent(
                    productState = cardState,
                    onAddToCart = { onAddToCart(product) },
                )
            }
        }
    }
}

@Composable
private fun ProductContent(
    productState: VioProductCardState,
    onAddToCart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val imageUrl = productState.primaryImage ?: productState.imageGallery.firstOrNull()
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = VioColors.backgroundMuted.toVioColor(),
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            VioImage(
                url = imageUrl,
                contentDescription = productState.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = VioColors.backgroundMuted.toVioColor(),
                        shape = RoundedCornerShape(16.dp),
                    ),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp),
        ) {
            Text(
                text = productState.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VioColors.textPrimary.toVioColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (productState.canShowDescription) {
                Text(
                    text = productState.description.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = VioColors.textSecondary.toVioColor(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

            Text(
                text = productState.priceLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VioColors.primary.toVioColor(),
            )

            Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = VioColors.primary.toVioColor(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Legg i kurv",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = VioColors.textOnPrimary.toVioColor(),
                )
            }
        }
    }
}

