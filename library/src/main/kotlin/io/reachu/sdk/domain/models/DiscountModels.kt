package io.reachu.sdk.domain.models

import com.fasterxml.jackson.annotation.JsonProperty

data class DiscountMetadataDto(
    @JsonProperty("apiKey") val apiKey: String? = null,
)

data class GetDiscountsDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("percentage") val percentage: Int? = null,
    @JsonProperty("discount_metadata") val discountMetadata: DiscountMetadataDto? = null,
)

data class GetDiscountByIdDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("percentage") val percentage: Int? = null,
)

data class GetDiscountTypeDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("type") val type: String = "",
)

data class AddDiscountDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("percentage") val percentage: Int? = null,
    @JsonProperty("start_date") val startDate: String? = null,
    @JsonProperty("end_date") val endDate: String? = null,
)

data class ApplyDiscountDto(
    @JsonProperty("executed") val executed: Boolean = false,
    @JsonProperty("message") val message: String = "",
)

data class DeleteAppliedDiscountDto(
    @JsonProperty("executed") val executed: Boolean = false,
    @JsonProperty("message") val message: String = "",
)

data class DeleteDiscountDto(
    @JsonProperty("executed") val executed: Boolean = false,
    @JsonProperty("message") val message: String = "",
)

data class UpdateDiscountDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("percentage") val percentage: Int? = null,
    @JsonProperty("start_date") val startDate: String? = null,
    @JsonProperty("end_date") val endDate: String? = null,
)

data class VerifyDiscountDto(
    @JsonProperty("valid") val valid: Boolean = false,
    @JsonProperty("message") val message: String = "",
    @JsonProperty("discount") val discount: GetDiscountByIdDto? = null,
)
