package com.reachu.tv2demo.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioUI.Components.VioProductCardConfig
import io.reachu.VioUI.Components.compose.product.VioProductCard
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Managers.toDomainProduct
import com.reachu.tv2demo.ui.theme.TV2Theme
import io.reachu.sdk.core.SdkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductsGridScreen(
    cartManager: CartManager,
    title: String = "Ukens tilbud",
    modifier: Modifier = Modifier,
) {
    val configurationState = VioConfiguration.shared.state.value
    val sdkClient = remember(configurationState.apiKey, configurationState.environment) {
        val apiKey = configurationState.apiKey.ifBlank { "DEMO_KEY" }
        SdkClient(URL(configurationState.environment.graphQLUrl), apiKey)
    }
    var state by rememberSaveable { mutableStateOf(ProductGridState(isLoading = true)) }

    LaunchedEffect(configurationState.market, cartManager.currency) {
        state = state.copy(isLoading = true, errorMessage = null)
        runCatching {
            loadProducts(
                sdkClient = sdkClient,
                currency = cartManager.currency,
                country = cartManager.country,
            )
        }.onSuccess { products ->
            state = state.copy(products = products, isLoading = false, errorMessage = null)
        }.onFailure { error ->
            state = state.copy(products = emptyList(), isLoading = false, errorMessage = error.localizedMessage ?: error.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TV2Theme.Colors.background),
    ) {
        ProductsToolbar(title = title)
        when {
            state.isLoading -> ProductsLoading()
            state.errorMessage != null -> ProductsError(message = state.errorMessage!!) {
                state = state.copy(isLoading = true, errorMessage = null)
            }
            state.products.isEmpty() -> ProductsEmpty()
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.products) { product ->
                        VioProductCard(
                            product = product,
                            variant = VioProductCardConfig.Variant.GRID,
                            onAddToCart = { variant: io.reachu.VioUI.Managers.Variant?, quantity: Int ->
                                cartManager.addProductAsync(product, quantity, variant)
                            },
                        )
                    }
                }
            }
        }
    }
}

private data class ProductGridState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

private suspend fun loadProducts(
    sdkClient: SdkClient,
    currency: String,
    country: String,
): List<Product> = withContext(Dispatchers.IO) {
    val dtos = sdkClient.channel.product.get(
        currency = currency,
        imageSize = "large",
        barcodeList = null,
        categoryIds = null,
        productIds = null,
        skuList = null,
        useCache = true,
        shippingCountryCode = country,
    )
    dtos.map { it.toDomainProduct() }
}

@Composable
private fun ProductsToolbar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TV2Theme.Typography.title,
            color = TV2Theme.Colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {}) {
            Icon(Icons.Filled.FilterList, contentDescription = null, tint = TV2Theme.Colors.textPrimary)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Filled.GridView, contentDescription = null, tint = TV2Theme.Colors.primary)
        }
    }
}

@Composable
private fun ProductsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = TV2Theme.Colors.primary)
    }
}

@Composable
private fun ProductsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Noe gikk galt", style = TV2Theme.Typography.title, color = Color.Red)
        Text(message, style = TV2Theme.Typography.body, color = TV2Theme.Colors.textSecondary, modifier = Modifier.padding(top = 16.dp))
        androidx.compose.material3.Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Prøv igjen")
        }
    }
}

@Composable
private fun ProductsEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ingen produkter", style = TV2Theme.Typography.title, color = TV2Theme.Colors.textPrimary)
        Text(
            "Det er ingen tilbud tilgjengelig for øyeblikket",
            style = TV2Theme.Typography.body,
            color = TV2Theme.Colors.textSecondary,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
