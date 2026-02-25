package io.reachu.liveui.components

import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.VioCore.models.Product
import io.reachu.liveshow.LiveShowCartManagerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VioLiveProductsComponentController(
    private val cartManager: LiveShowCartManaging = LiveShowCartManagerProvider.default,
    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Main),
) {
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _showGrid = MutableStateFlow(false)
    val showGrid: StateFlow<Boolean> = _showGrid.asStateFlow()

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun clearSelection() {
        _selectedProduct.value = null
    }

    fun toggleGrid() {
        _showGrid.value = !_showGrid.value
    }

    fun addToCart(product: Product) {
        scope.launch {
            cartManager.addProduct(product, quantity = 1)
        }
    }

    fun addSelectedProductToCart() {
        _selectedProduct.value?.let { addToCart(it) }
    }
}
