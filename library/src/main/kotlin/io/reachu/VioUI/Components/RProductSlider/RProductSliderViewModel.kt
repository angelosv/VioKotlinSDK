package io.reachu.VioUI.Components.slider

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.utils.VioLogger
import io.reachu.VioUI.Components.VioProductSliderState
import io.reachu.VioUI.Services.ProductService
import io.reachu.VioUI.Services.ProductServiceError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class VioProductSliderViewModel(
    private val productService: ProductService = ProductService,
    private val logger: (String) -> Unit = { VioLogger.debug(it, "VioProductSlider") },
) {

    private val _state = MutableStateFlow(VioProductSliderState())
    val state: StateFlow<VioProductSliderState> = _state.asStateFlow()

    private var lastRequestKey: Triple<Int?, String, String>? = null
    private var hasLoaded: Boolean = false

    suspend fun loadProducts(
        categoryId: Int? = null,
        currency: String = "USD",
        country: String = "US",
        forceRefresh: Boolean = false,
    ) {
        if (!VioConfiguration.shared.shouldUseSDK) {
            logger("⚠️ Skipping load - SDK disabled (market unavailable)")
            _state.value = VioProductSliderState(
                products = emptyList(),
                isLoading = false,
                errorMessage = null,
                isMarketUnavailable = true,
            )
            return
        }

        val requestKey = Triple(categoryId, currency, country)
        if (!forceRefresh && hasLoaded && lastRequestKey == requestKey) return
        if (_state.value.isLoading) return

        if (forceRefresh) {
            hasLoaded = false
            _state.value = VioProductSliderState(isLoading = true)
        } else {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, isMarketUnavailable = false)
        }

        logger("🛍️ Loading products… currency=$currency country=$country category=${categoryId ?: "all"}")

        try {
            val products = withContext(Dispatchers.IO) {
                val loader = suspend {
                    if (categoryId != null) {
                        productService.loadProductsByCategory(categoryId, currency, country)
                    } else {
                        productService.loadProducts(productIds = null, currency = currency, country = country)
                    }
                }
                withTimeoutOrNull(20_000) { loader() } ?: throw ProductServiceError.Network(
                    IllegalStateException("Timeout while loading products"),
                )
            }
            hasLoaded = true
            lastRequestKey = requestKey
            _state.value = _state.value.copy(
                products = products,
                isLoading = false,
                errorMessage = null,
                isMarketUnavailable = false,
            )
        } catch (error: ProductServiceError) {
            when (error) {
                is ProductServiceError.Sdk -> {
                    val status = error.error.status
                    val code = error.error.code
                    val marketUnavailable = status == 404 || code?.equals("NOT_FOUND", ignoreCase = true) == true
                    _state.value = _state.value.copy(
                        products = emptyList(),
                        isLoading = false,
                        errorMessage = if (marketUnavailable) null else error.error.messageText,
                        isMarketUnavailable = marketUnavailable,
                    )
                }
                is ProductServiceError.Network -> {
                    _state.value = _state.value.copy(
                        products = emptyList(),
                        isLoading = false,
                        errorMessage = error.message,
                        isMarketUnavailable = false,
                    )
                }
                is ProductServiceError.InvalidConfiguration,
                is ProductServiceError.InvalidProductId,
                is ProductServiceError.ProductNotFound -> {
                    _state.value = _state.value.copy(
                        products = emptyList(),
                        isLoading = false,
                        errorMessage = error.message,
                        isMarketUnavailable = false,
                    )
                }
            }
            hasLoaded = false
            lastRequestKey = null
        }
    }

    suspend fun reload(
        categoryId: Int? = null,
        currency: String = "USD",
        country: String = "US",
    ) {
        hasLoaded = false
        loadProducts(categoryId, currency, country, forceRefresh = true)
    }
}
