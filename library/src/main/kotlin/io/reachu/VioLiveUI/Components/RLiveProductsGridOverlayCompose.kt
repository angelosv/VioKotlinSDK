package io.reachu.liveui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.asProduct
import io.reachu.liveshow.models.formattedCompareAtPrice
import io.reachu.liveshow.models.formattedPrice

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VioLiveProductsGridOverlay(
    products: List<LiveProduct>,
    controller: VioLiveProductsComponentController = remember { VioLiveProductsComponentController() },
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
) {
    val selected by controller.selectedProduct.collectAsState()
    Column(
        modifier = modifier
            .background(Color(0xFF111111), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(bottom = 24.dp),
    ) {
        DragIndicatorHeader(onClose = onClose, count = products.size)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(products) { product ->
                ProductGridItem(
                    product = product,
                    imageLoader = imageLoader,
                    onClick = {
                        controller.selectProduct(product.asProduct)
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            selected?.let {
                SelectedProductSheet(
                    product = it,
                    onAddToCart = controller::addSelectedProductToCart,
                    onDismiss = {
                        controller.clearSelection()
                        onClose()
                    },
                )
            }
        }
    }
}

@Composable
private fun DragIndicatorHeader(
    onClose: () -> Unit,
    count: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(0.15f),
            content = {},
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Featured products",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Live shopping • $count items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close grid",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ProductGridItem(
    product: LiveProduct,
    imageLoader: VioImageLoader,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VioImage(
                url = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                imageLoader = imageLoader,
            )
            Text(
                text = product.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = product.price.formattedPrice(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                product.originalPrice?.formattedCompareAtPrice()?.let { compare ->
                    Text(
                        text = compare,
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            product.discount?.takeIf { it.isNotBlank() }?.let { discount ->
                Text(
                    text = discount,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(Color.Red, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
