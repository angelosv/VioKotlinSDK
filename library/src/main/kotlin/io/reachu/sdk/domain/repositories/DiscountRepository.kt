package io.reachu.sdk.domain.repositories

import io.reachu.sdk.domain.models.AddDiscountDto
import io.reachu.sdk.domain.models.ApplyDiscountDto
import io.reachu.sdk.domain.models.DeleteAppliedDiscountDto
import io.reachu.sdk.domain.models.DeleteDiscountDto
import io.reachu.sdk.domain.models.GetDiscountByIdDto
import io.reachu.sdk.domain.models.GetDiscountTypeDto
import io.reachu.sdk.domain.models.GetDiscountsDto
import io.reachu.sdk.domain.models.UpdateDiscountDto
import io.reachu.sdk.domain.models.VerifyDiscountDto

interface DiscountRepository {
    suspend fun get(): List<GetDiscountsDto>
    suspend fun getByChannel(): List<GetDiscountsDto>
    suspend fun getById(discountId: Int): GetDiscountByIdDto
    suspend fun getType(id: Int? = null, type: String? = null): List<GetDiscountTypeDto>
    suspend fun add(code: String, percentage: Int, startDate: String, endDate: String, typeId: Int): AddDiscountDto
    suspend fun apply(code: String, cartId: String): ApplyDiscountDto
    suspend fun deleteApplied(code: String, cartId: String): DeleteAppliedDiscountDto
    suspend fun delete(discountId: Int): DeleteDiscountDto
    suspend fun update(
        discountId: Int,
        code: String? = null,
        percentage: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        products: List<Int>? = null,
    ): UpdateDiscountDto
    suspend fun verify(verifyDiscountId: Int?, code: String?): VerifyDiscountDto
}
