package io.reachu.sdk.domain.repositories

import io.reachu.sdk.domain.models.CartDto
import io.reachu.sdk.domain.models.GetLineItemsBySupplierDto
import io.reachu.sdk.domain.models.LineItemInput
import io.reachu.sdk.domain.models.RemoveCartDto

interface CartRepository {
    suspend fun getById(cartId: String): CartDto
    suspend fun create(customerSessionId: String, currency: String, shippingCountry: String? = null): CartDto
    suspend fun update(cartId: String, shippingCountry: String): CartDto
    suspend fun delete(cartId: String): RemoveCartDto
    suspend fun addItem(cartId: String, lineItems: List<LineItemInput>): CartDto
    suspend fun updateItem(cartId: String, cartItemId: String, shippingId: String? = null, quantity: Int? = null): CartDto
    suspend fun deleteItem(cartId: String, cartItemId: String): CartDto
    suspend fun getLineItemsBySupplier(cartId: String): List<GetLineItemsBySupplierDto>
}
