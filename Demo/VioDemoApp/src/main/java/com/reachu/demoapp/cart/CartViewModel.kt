package com.reachu.demoapp.cart

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.reachu.VioUI.Managers.CartItem
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Managers.clearCart
import io.reachu.VioUI.Managers.discountApplyOrCreate
import io.reachu.VioUI.Managers.discountRemoveApplied
import io.reachu.VioUI.Managers.Variant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * ViewModel que traduce CartManager → CartUiState.
 * Usa snapshotFlow para mantener compatibilidad con mutableStateOf interno.
 */
class CartViewModel(
    private val cartManager: CartManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                snapshotFlow { cartManager.items },
                snapshotFlow { cartManager.subtotal },
                snapshotFlow { cartManager.shippingTotal },
                snapshotFlow { cartManager.discountTotal },
                snapshotFlow { cartManager.taxTotal },
                snapshotFlow { cartManager.cartTotal },
                snapshotFlow { cartManager.isLoading },
                snapshotFlow { cartManager.errorMessage },
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                CartUiState(
                    items = array[0] as List<CartItem>,
                    subtotal = array[1] as Double,
                    shipping = array[2] as Double,
                    discount = array[3] as Double,
                    tax = array[4] as Double,
                    total = array[5] as Double,
                    discountCode = cartManager.lastDiscountCode,
                    isLoading = array[6] as Boolean,
                    error = array[7] as String?,
                )
            }.distinctUntilChanged().collect { _uiState.value = it }
        }
    }

    fun add(product: Product, variant: Variant? = null, quantity: Int = 1) = cartManager.addProductAsync(product, quantity, variant)
    fun increment(item: CartItem) = cartManager.updateQuantityAsync(item, item.quantity + 1)
    fun decrement(item: CartItem) = cartManager.updateQuantityAsync(item, (item.quantity - 1).coerceAtLeast(0))
    fun remove(item: CartItem) = cartManager.removeItemAsync(item)
    fun clear() = viewModelScope.launch { cartManager.clearCart() }
    fun applyDiscount(code: String) = viewModelScope.launch { cartManager.discountApplyOrCreate(code) }
    fun removeDiscount() = viewModelScope.launch { cartManager.discountRemoveApplied() }
}

class CartViewModelFactory(
    private val cartManager: CartManager,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(cartManager) as T
        }
        throw IllegalArgumentException("Unknown CartViewModel class")
    }
}
