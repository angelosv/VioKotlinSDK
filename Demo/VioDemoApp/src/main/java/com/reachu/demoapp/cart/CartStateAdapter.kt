package com.reachu.demoapp.cart

import androidx.compose.runtime.snapshotFlow
import io.reachu.VioUI.Managers.CartManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Adaptador simple para exponer CartManager como Flow<CartUiState>.
 * Permite migrar gradualmente hasta que el SDK exponga StateFlow nativo.
 */
fun CartManager.asCartStateFlow(): Flow<CartUiState> =
    combine(
        snapshotFlow { items },
        snapshotFlow { cartTotal },
        snapshotFlow { shippingTotal },
        snapshotFlow { isLoading },
        snapshotFlow { errorMessage },
    ) { items, subtotal, shipping, loading, error ->
        CartUiState(
            items = items,
            subtotal = subtotal,
            shipping = shipping,
            total = subtotal + shipping,
            discountCode = lastDiscountCode,
            isLoading = loading,
            error = error,
        )
    }
