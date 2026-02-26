package io.reachu.demo.demos

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.VioSdkClient
import io.reachu.sdk.core.errors.SdkException
import io.reachu.sdk.core.helpers.JsonUtils
import kotlin.math.min

suspend fun runChannelProductDemo(config: DemoConfig) {
    val currency = config.currency
    val country = config.country
    val imageSize = "large"
    val productId = 402517

    val sdk = VioSdkClient(config.baseUrl, config.apiToken)
    val products = sdk.channel.product
    val categoriesRepo = sdk.channel.category

    try {
        Logger.section("Product.getByParams (productId=$productId)")
        val (single, _) = Logger.measure("Product.getByParams") {
            products.getByParams(
                currency = currency,
                imageSize = imageSize,
                sku = null,
                barcode = null,
                productId = productId,
                shippingCountryCode = country,
            )
        }
        Logger.json(single, "Response (Product.getByParams)")

        val productMap = JsonUtils.convert<Map<String, Any?>>(single)
        val foundSku = productMap["sku"] as? String
        val foundBarcode = productMap["barcode"] as? String

        if (!foundSku.isNullOrBlank()) {
            Logger.section("Product.getBySkus ($foundSku)")
            val (bySku, _) = Logger.measure("Product.getBySkus") {
                products.getBySkus(
                    sku = foundSku,
                    productId = null,
                    currency = currency,
                    imageSize = imageSize,
                    shippingCountryCode = country,
                )
            }
            Logger.json(bySku, "Response (Product.getBySkus)")
        } else {
            Logger.section("Product.getBySkus (skipped)")
            Logger.warn("No SKU found for product $productId")
        }

        if (!foundBarcode.isNullOrBlank()) {
            Logger.section("Product.getByBarcodes ($foundBarcode)")
            val (byBarcode, _) = Logger.measure("Product.getByBarcodes") {
                products.getByBarcodes(
                    barcode = foundBarcode,
                    productId = null,
                    currency = currency,
                    imageSize = imageSize,
                    shippingCountryCode = country,
                )
            }
            Logger.json(byBarcode, "Response (Product.getByBarcodes)")
        } else {
            Logger.section("Product.getByBarcodes (skipped)")
            Logger.warn("No barcode found for product $productId")
        }

        Logger.section("Product.getByIds")
        val (byIds, _) = Logger.measure("Product.getByIds") {
            products.getByIds(
                productIds = listOf(productId),
                currency = currency,
                imageSize = imageSize,
                useCache = true,
                shippingCountryCode = country,
            )
        }
        Logger.json(byIds, "Response (Product.getByIds)")

        Logger.section("Product.get (filtered by productIds)")
        val (generic, _) = Logger.measure("Product.get") {
            products.get(
                currency = currency,
                imageSize = imageSize,
                barcodeList = null,
                categoryIds = null,
                productIds = listOf(productId),
                skuList = null,
                useCache = true,
                shippingCountryCode = country,
            )
        }
        Logger.json(generic, "Response (Product.get)")

        Logger.section("Category.get")
        val (categories, _) = Logger.measure("Category.get") {
            categoriesRepo.get()
        }
        Logger.json(categories, "Response (Category.get)")

        val firstCategory = categories.firstOrNull()
        val categoryId = firstCategory?.let {
            val map = JsonUtils.convert<Map<String, Any?>>(it)
            (map["id"] as? Number)?.toInt() ?: (map["category_id"] as? Number)?.toInt()
        }

        if (categoryId != null && categoryId > 0) {
            Logger.section("Product.getByCategoryId ($categoryId)")
            val (byCategoryId, _) = Logger.measure("Product.getByCategoryId") {
                products.getByCategoryId(
                    categoryId = categoryId,
                    currency = currency,
                    imageSize = imageSize,
                    shippingCountryCode = country,
                )
            }
            Logger.json(byCategoryId, "Response (Product.getByCategoryId)")

            val catIds = categories
                .take(min(3, categories.size))
                .mapNotNull {
                    val map = JsonUtils.convert<Map<String, Any?>>(it)
                    (map["id"] as? Number)?.toInt() ?: (map["category_id"] as? Number)?.toInt()
                }

            if (catIds.isNotEmpty()) {
                Logger.section("Product.getByCategoryIds $catIds")
                val (byCategoryIds, _) = Logger.measure("Product.getByCategoryIds") {
                    products.getByCategoryIds(
                        categoryIds = catIds,
                        currency = currency,
                        imageSize = imageSize,
                        shippingCountryCode = country,
                    )
                }
                Logger.json(byCategoryIds, "Response (Product.getByCategoryIds)")
            } else {
                Logger.section("Product.getByCategoryIds (skipped)")
                Logger.warn("Could not extract category IDs from response.")
            }
        } else {
            Logger.section("Product.getByCategoryId (skipped)")
            Logger.warn("No category ID available in first category.")
        }

        Logger.section("Done")
        Logger.success("Product demo finished successfully.")
    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

