package com.vio.demoapp.cart

import live.vio.VioUI.Managers.CartItem

/**
 * Estado comprimido para la UI de carrito.
 * Permite desacoplar Compose del CartManager directo.
 */
data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val discountCode: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
