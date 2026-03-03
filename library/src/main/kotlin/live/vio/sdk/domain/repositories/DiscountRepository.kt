package live.vio.sdk.domain.repositories

import live.vio.sdk.domain.models.AddDiscountDto
import live.vio.sdk.domain.models.ApplyDiscountDto
import live.vio.sdk.domain.models.DeleteAppliedDiscountDto
import live.vio.sdk.domain.models.DeleteDiscountDto
import live.vio.sdk.domain.models.GetDiscountByIdDto
import live.vio.sdk.domain.models.GetDiscountTypeDto
import live.vio.sdk.domain.models.GetDiscountsDto
import live.vio.sdk.domain.models.UpdateDiscountDto
import live.vio.sdk.domain.models.VerifyDiscountDto

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
