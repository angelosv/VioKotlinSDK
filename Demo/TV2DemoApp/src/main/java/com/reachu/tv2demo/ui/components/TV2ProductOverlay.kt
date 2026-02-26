package com.reachu.tv2demo.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.reachu.sdk.core.VioSdkClient
import io.reachu.sdk.domain.models.ProductDto
import com.reachu.tv2demo.services.events.ProductEventData
import com.reachu.tv2demo.ui.theme.TV2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TV2ProductOverlay(
    productEvent: ProductEventData,
    sdk: VioSdkClient,
    currency: String,
    country: String,
    onAddToCart: (ProductDto?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var product by remember(productEvent.id) { mutableStateOf<ProductDto?>(null) }
    var isLoading by remember(productEvent.id) { mutableStateOf(true) }

    LaunchedEffect(productEvent.id) {
        isLoading = true
        product = fetchProduct(productEvent, sdk, currency, country)
        isLoading = false
    }

    val bottomPadding = if (isLandscape) {
        140.dp
    } else {
        200.dp
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Produkt",
                    style = TV2Theme.Typography.title,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                }
            }

            TV2SponsorBadge(
                logoUrl = productEvent.campaignLogo,
                modifier = Modifier.align(Alignment.Start),
            )

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Laster produkt …", style = TV2Theme.Typography.body, color = Color.White.copy(alpha = 0.8f))
                }
            } else {
                ProductBody(
                    productEvent = productEvent,
                    product = product,
                    onAddToCart = onAddToCart,
                )
            }
        }
    }
}

@Composable
private fun ProductBody(
    productEvent: ProductEventData,
    product: ProductDto?,
    onAddToCart: (ProductDto?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val imageUrl = product?.images?.firstOrNull()?.url ?: productEvent.imageUrl
        io.reachu.VioUI.Components.compose.product.VioImage(
            url = imageUrl,
            contentDescription = product?.title ?: productEvent.name,
            modifier = Modifier
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = product?.title ?: productEvent.name,
                style = TV2Theme.Typography.title.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            if (!displayDescription(product, productEvent).isNullOrEmpty()) {
                Text(
                    text = displayDescription(product, productEvent),
                    style = TV2Theme.Typography.body,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Text(
                text = displayPrice(product, productEvent),
                style = TV2Theme.Typography.title,
                color = TV2Theme.Colors.primary,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TV2Theme.Colors.primary, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp)
                    .clickable { onAddToCart(product) },
                contentAlignment = Alignment.Center,
            ) {
                Text("Legg i kurv", style = TV2Theme.Typography.body.copy(fontWeight = FontWeight.Bold), color = Color.Black)
            }
        }
    }
}

private fun displayDescription(product: ProductDto?, event: ProductEventData): String {
    val source = product?.description ?: event.description
    return source
        .replace("<[^>]+>".toRegex(), " ")
        .replace("&nbsp;", " ")
        .trim()
}

private fun displayPrice(product: ProductDto?, event: ProductEventData): String {
    return if (product != null) {
        val value = product.price.amountInclTaxes ?: product.price.amount
        "${product.price.currencyCode} ${"%.0f".format(value)}"
    } else {
        "${event.currency} ${event.price}"
    }
}

private suspend fun fetchProduct(
    productEvent: ProductEventData,
    sdk: VioSdkClient,
    currency: String,
    country: String,
): ProductDto? = withContext(Dispatchers.IO) {
    val productId = productEvent.productId.toIntOrNull() ?: return@withContext null
    runCatching {
        sdk.channel.product.getByIds(
            productIds = listOf(productId),
            currency = currency,
            imageSize = "large",
            useCache = false,
            shippingCountryCode = country,
        )
    }.getOrNull()?.firstOrNull()
}
