package io.reachu.liveui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.VioCore.models.Product
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.liveshow.LiveShowCartManagerProvider
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.asProduct
import io.reachu.liveshow.models.formattedCompareAtPrice
import io.reachu.liveshow.models.formattedPrice

@Composable
fun VioLiveProductsComponent(
    products: List<LiveProduct>,
    manager: LiveShowManager = LiveShowManager.shared,
    cartManager: LiveShowCartManaging = LiveShowCartManagerProvider.default,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    onProductSelected: (Product) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Featured Products", color = Color.White)
            Text(text = "${products.size} items", color = Color.White.copy(alpha = 0.7f))
        }
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(products) { product ->
                LiveProductTile(
                    product = product,
                    imageLoader = imageLoader,
                    onAdd = { manager.addProductToCart(product, cartManager) },
                    onSelected = { onProductSelected(product.asProduct) },
                )
            }
        }
    }
}

@Composable
private fun LiveProductTile(
    product: LiveProduct,
    imageLoader: VioImageLoader,
    onAdd: () -> Unit,
    onSelected: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .height(210.dp),
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        onClick = onSelected,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VioImage(
                url = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                imageLoader = imageLoader,
            )
            Text(
                text = product.title,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = product.price.formattedPrice(),
                    color = Color(0xFFFF4D73),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
                product.originalPrice?.formattedCompareAtPrice()?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            product.discount?.takeIf { it.isNotBlank() }?.let { discount ->
                Text(
                    text = discount,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(Color.Red, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add")
            }
        }
    }
}
