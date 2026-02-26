package com.reachu.viaplaydemo.viewmodel

import io.reachu.VioUI.Managers.Product
import io.reachu.sdk.core.VioSdkClient
import io.reachu.sdk.domain.models.ProductDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kotlin counterpart of `ProductFetchViewModel.swift`.
 * Fetches Vio products by ID so the overlays can show details instantly.
 */
class ProductFetchViewModel(
    private val sdk: VioSdkClient,
    private val currency: String,
    private val country: String,
) {

    data class State(
        val product: ProductDto? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun fetchProduct(productId: String) {
        scope.launch {
            if (productId.isBlank()) {
                _state.value = State(errorMessage = "Invalid productId")
                return@launch
            }
            _state.value = State(isLoading = true)
            val result = runCatching { fetchProductsInternal(listOf(productId)) }
            val newState = result.fold(
                onSuccess = { products ->
                    State(product = products.firstOrNull(), isLoading = false)
                },
                onFailure = { error ->
                    State(isLoading = false, errorMessage = error.localizedMessage)
                },
            )
            _state.value = newState
        }
    }

    fun fetchProducts(ids: List<String>) {
        scope.launch {
            if (ids.isEmpty()) {
                _state.value = State(errorMessage = "IDs list is empty")
                return@launch
            }
            _state.value = State(isLoading = true)
            val result = runCatching { fetchProductsInternal(ids) }
            val newState = result.fold(
                onSuccess = { products ->
                    State(product = products.firstOrNull(), isLoading = false)
                },
                onFailure = { error ->
                    State(isLoading = false, errorMessage = error.localizedMessage)
                },
            )
            _state.value = newState
        }
    }

    private suspend fun fetchProductsInternal(ids: List<String>): List<ProductDto> =
        withContext(Dispatchers.IO) {
            val productIds = ids.mapNotNull { it.toIntOrNull() }
            require(productIds.size == ids.size) { "Some product IDs are not numeric" }
            sdk.channel.product.getByIds(
                productIds = productIds,
                currency = currency,
                imageSize = "large",
                useCache = false,
                shippingCountryCode = country,
            )
        }
}
