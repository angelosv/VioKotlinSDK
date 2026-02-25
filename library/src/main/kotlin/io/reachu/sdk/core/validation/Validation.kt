package io.reachu.sdk.core.validation

import io.reachu.sdk.core.errors.ValidationException

object Validation {
    private val iso4217 = Regex("^[A-Z]{3}$")
    private val iso3166a2 = Regex("^[A-Z]{2}$")

    fun requireNonEmpty(value: String?, field: String) {
        if (value == null || value.trim().isEmpty()) {
            throw ValidationException("Required field", mapOf("field" to field))
        }
    }

    fun requireCurrency(currency: String) {
        if (!iso4217.matches(currency)) {
            throw ValidationException(
                "currency must be ISO-4217 (3 uppercase letters)",
                mapOf("field" to "currency", "got" to currency),
            )
        }
    }

    fun requireCountry(code: String) {
        if (!iso3166a2.matches(code)) {
            throw ValidationException(
                "countryCode must be ISO-3166-1 alpha-2 (2 letters)",
                mapOf("field" to "countryCode", "got" to code),
            )
        }
    }
}
