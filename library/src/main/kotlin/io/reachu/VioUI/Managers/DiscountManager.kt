package io.reachu.VioUI.Managers

import io.reachu.sdk.core.errors.SdkException

suspend fun CartManager.discountCreate(
    code: String,
    percentage: Int,
    startDate: String? = null,
    endDate: String? = null,
    typeId: Int = 2,
): Int? {
    isLoading = true
    errorMessage = null
    return try {
        logRequest(
            "sdk.discount.add",
            mapOf(
                "code" to code,
                "percentage" to percentage,
                "startDate" to startDate,
                "endDate" to endDate,
                "typeId" to typeId,
            ),
        )
        val start = startDate ?: iso8601String()
        val end = endDate ?: java.time.Instant.now().plus(java.time.Duration.ofDays(7)).toString()
        val dto = sdk.discount.add(
            code = code,
            percentage = percentage,
            startDate = start,
            endDate = end,
            typeId = typeId,
        )
        logResponse("sdk.discount.add", mapOf("discountId" to dto.id))
        lastDiscountId = dto.id
        lastDiscountCode = code
        ToastManager.showSuccess("Discount created: $code")
        dto.id
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.discount.add", sdkError)
        println("❌ [Discount] create FAIL $msg")
        ToastManager.showError("Create discount failed")
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.discount.add", t)
        println("❌ [Discount] create FAIL $msg")
        ToastManager.showError("Create discount failed")
        null
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.discountApply(code: String): Boolean {
    isLoading = true
    errorMessage = null
    val normalized = code.trim().uppercase()
    if (normalized.isEmpty()) {
        println("ℹ️ [Discount] apply: missing code")
        isLoading = false
        return false
    }

    val cid = ensureCartIDForCheckout()
        ?: run {
            println("ℹ️ [Discount] apply: missing cartId")
            isLoading = false
            return false
        }

    return try {
        logRequest("sdk.discount.apply", mapOf("code" to normalized, "cartId" to cid))
        val dto = sdk.discount.apply(code = normalized, cartId = cid)
        logResponse(
            "sdk.discount.apply",
            mapOf("executed" to dto.executed, "message" to dto.message),
        )
        if (dto.executed) {
            lastDiscountCode = normalized
            val message = dto.message?.takeIf { it.isNotBlank() } ?: "Discount applied: $normalized"
            ToastManager.showSuccess(message)
            refreshCheckoutTotals()
            true
        } else {
            errorMessage = dto.message
            println("⚠️ [Discount] apply NOT EXECUTED ($normalized) -> ${dto.message}")
            val message = dto.message?.takeIf { it.isNotBlank() } ?: "Discount not applied"
            ToastManager.showInfo(message)
            false
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.discount.apply", sdkError)
        println("❌ [Discount] apply FAIL $msg")
        ToastManager.showError("Apply discount failed")
        false
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.discount.apply", t)
        println("❌ [Discount] apply FAIL $msg")
        ToastManager.showError("Apply discount failed")
        false
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.discountRemoveApplied(code: String? = null): Boolean {
    isLoading = true
    errorMessage = null
    val cid = ensureCartIDForCheckout()
        ?: run {
            println("ℹ️ [Discount] deleteApplied: missing cartId")
            isLoading = false
            return false
        }

    val useCode = (code ?: lastDiscountCode).orEmpty().trim().uppercase()
    if (useCode.isEmpty()) {
        println("ℹ️ [Discount] deleteApplied: missing code")
        isLoading = false
        return false
    }

    return try {
        logRequest("sdk.discount.deleteApplied", mapOf("code" to useCode, "cartId" to cid))
        sdk.discount.deleteApplied(code = useCode, cartId = cid)
        if (lastDiscountCode == useCode) lastDiscountCode = null
        ToastManager.showInfo("Discount removed: $useCode")
        refreshCheckoutTotals()
        true
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.discount.deleteApplied", sdkError)
        println("❌ [Discount] deleteApplied FAIL $msg")
        ToastManager.showError("Remove discount failed")
        false
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.discount.deleteApplied", t)
        println("❌ [Discount] deleteApplied FAIL $msg")
        ToastManager.showError("Remove discount failed")
        false
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.discountDelete(discountId: Int): Boolean {
    isLoading = true
    errorMessage = null
    return try {
        logRequest("sdk.discount.delete", mapOf("discountId" to discountId))
        sdk.discount.delete(discountId = discountId)
        if (lastDiscountId == discountId) lastDiscountId = null
        ToastManager.showInfo("Discount deleted: $discountId")
        true
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        logError("sdk.discount.delete", sdkError)
        println("❌ [Discount] delete FAIL $msg")
        ToastManager.showError("Delete discount failed")
        false
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        logError("sdk.discount.delete", t)
        println("❌ [Discount] delete FAIL $msg")
        ToastManager.showError("Delete discount failed")
        false
    } finally {
        isLoading = false
    }
}

suspend fun CartManager.discountGetIdByCode(code: String): Int? {
    val needle = code.trim()
    if (needle.isEmpty()) return null

    return try {
        logRequest("sdk.discount.getByChannel")
        val channelList = sdk.discount.getByChannel()
        logResponse("sdk.discount.getByChannel", mapOf("count" to channelList.size))
        channelList.firstOrNull {
            (it.code ?: "").equals(needle, ignoreCase = true)
        }?.let {
            lastDiscountId = it.id
            lastDiscountCode = it.code
            return it.id
        }

        logRequest("sdk.discount.get")
        val all = sdk.discount.get()
        logResponse("sdk.discount.get", mapOf("count" to all.size))
        all.firstOrNull {
            (it.code ?: "").equals(needle, ignoreCase = true)
        }?.let {
            lastDiscountId = it.id
            lastDiscountCode = it.code
            it.id
        }
    } catch (sdkError: SdkException) {
        val msg = sdkError.messageText
        errorMessage = msg
        println("⚠️ [Discount] get by code '$code' FAIL $msg")
        logError("sdk.discount.get", sdkError)
        null
    } catch (t: Throwable) {
        val msg = t.message
        errorMessage = msg
        println("⚠️ [Discount] get by code '$code' FAIL $msg")
        logError("sdk.discount.get", t)
        null
    }
}

suspend fun CartManager.discountApplyOrCreate(
    code: String,
    percentage: Int = 10,
    startDate: String? = null,
    endDate: String? = null,
    typeId: Int = 2,
): Boolean {
    val normalized = code.trim().uppercase()
    if (normalized.isEmpty()) return false

    if (discountApply(normalized)) return true

    if (discountGetIdByCode(normalized) != null) {
        if (discountApply(normalized)) return true
        return false
    }

    if (discountCreate(
            code = normalized,
            percentage = percentage,
            startDate = startDate,
            endDate = endDate,
            typeId = typeId,
        ) != null
    ) {
        return discountApply(normalized)
    }

    return false
}
