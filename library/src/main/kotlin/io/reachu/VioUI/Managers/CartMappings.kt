package io.reachu.VioUI.Managers

import io.reachu.VioCore.models.BasePrice as CoreBasePrice
import io.reachu.VioCore.models.Category as CoreCategory
import io.reachu.VioCore.models.Option as CoreOption
import io.reachu.VioCore.models.Price as CorePrice
import io.reachu.VioCore.models.Product as CoreProduct
import io.reachu.VioCore.models.ProductImage as CoreProductImage
import io.reachu.VioCore.models.ProductShipping as CoreProductShipping
import io.reachu.VioCore.models.ReturnAddress as CoreReturnAddress
import io.reachu.VioCore.models.ReturnInfo as CoreReturnInfo
import io.reachu.VioCore.models.ShippingCountry as CoreShippingCountry
import io.reachu.VioCore.models.Variant as CoreVariant
import io.reachu.sdk.domain.models.CategoryDto
import io.reachu.sdk.domain.models.MarketDto
import io.reachu.sdk.domain.models.OptionDto
import io.reachu.sdk.domain.models.PriceDto
import io.reachu.sdk.domain.models.ProductDto
import io.reachu.sdk.domain.models.ProductImageDto
import io.reachu.sdk.domain.models.ProductShippingDto
import io.reachu.sdk.domain.models.ReturnAddressDto
import io.reachu.sdk.domain.models.ReturnInfoDto
import io.reachu.sdk.domain.models.ShippingCountryDto
import io.reachu.sdk.domain.models.VariantDto

/**
 * Kotlin counterpart for the Swift mapping helpers used by `CartManager`.
 * These data classes mirror the ones defined in VioCore for compatibility.
 */
data class Price(
    val amount: Float,
    val currencyCode: String,
    val amountInclTaxes: Float? = null,
    val taxAmount: Float? = null,
    val taxRate: Float? = null,
    val compareAt: Float? = null,
    val compareAtInclTaxes: Float? = null,
)

data class BasePrice(
    val amount: Float,
    val currencyCode: String,
    val amountInclTaxes: Float? = null,
    val taxAmount: Float? = null,
    val taxRate: Float? = null,
)

data class ProductImage(
    val id: String,
    val url: String,
    val width: Int?,
    val height: Int?,
    val order: Int,
)

data class Option(
    val id: String,
    val name: String,
    val order: Int?,
    val values: String?,
)

data class Category(
    val id: Int,
    val name: String,
)

data class ShippingCountry(
    val id: String,
    val country: String,
    val price: BasePrice,
)

data class ProductShipping(
    val id: String,
    val name: String,
    val description: String?,
    val customPriceEnabled: Boolean,
    val isDefault: Boolean,
    val shippingCountry: List<ShippingCountry>?,
)

data class ReturnAddress(
    val sameAsBusiness: Boolean?,
    val sameAsWarehouse: Boolean?,
    val country: String?,
    val timezone: String?,
    val address: String?,
    val address2: String?,
    val postCode: String?,
    val returnCity: String?,
)

data class ReturnInfo(
    val returnRight: Boolean?,
    val returnLabel: String?,
    val returnCost: Float?,
    val supplierPolicy: String?,
    val returnAddress: ReturnAddress?,
)

data class Variant(
    val id: String,
    val barcode: String?,
    val price: Price,
    val quantity: Int?,
    val sku: String,
    val title: String,
    val images: List<ProductImage>,
)

data class Product(
    val id: Int,
    val title: String,
    val brand: String?,
    val description: String?,
    val tags: String?,
    val sku: String,
    val quantity: Int?,
    val price: Price,
    val variants: List<Variant>,
    val barcode: String?,
    val options: List<Option>?,
    val categories: List<Category>?,
    val images: List<ProductImage>,
    val productShipping: List<ProductShipping>?,
    val supplier: String,
    val supplierId: Int?,
    val importedProduct: Boolean?,
    val referralFee: Int?,
    val optionsEnabled: Boolean,
    val digital: Boolean,
    val origin: String,
    val returnInfo: ReturnInfo?,
)

fun ProductDto.toDomainProduct(): Product = Product(
    id = id,
    title = title,
    brand = brand,
    description = description,
    tags = tags,
    sku = sku,
    quantity = quantity,
    price = price.toDomainPrice(),
    variants = variants.map { it.toDomainVariant() },
    barcode = barcode,
    options = options.takeIf { it.isNotEmpty() }?.map { it.toDomainOption() },
    categories = categories?.map { it.toDomainCategory() },
    images = images.map { it.toDomainImage() },
    productShipping = productShipping?.map { it.toDomainProductShipping() },
    supplier = supplier,
    supplierId = supplierId,
    importedProduct = importedProduct,
    referralFee = referralFee,
    optionsEnabled = optionsEnabled,
    digital = digital,
    origin = origin,
    returnInfo = returnInfo?.toDomainReturnInfo(),
)

fun PriceDto.toDomainPrice(): Price = Price(
    amount = amount.toFloat(),
    currencyCode = currencyCode,
    amountInclTaxes = amountInclTaxes?.toFloat(),
    taxAmount = taxAmount?.toFloat(),
    taxRate = taxRate?.toFloat(),
    compareAt = compareAt?.toFloat(),
    compareAtInclTaxes = compareAtInclTaxes?.toFloat(),
)

fun PriceDto.toDomainBasePrice(): BasePrice = BasePrice(
    amount = amount.toFloat(),
    currencyCode = currencyCode,
    amountInclTaxes = amountInclTaxes?.toFloat(),
    taxAmount = taxAmount?.toFloat(),
    taxRate = taxRate?.toFloat(),
)

fun VariantDto.toDomainVariant(): Variant = Variant(
    id = id,
    barcode = barcode,
    price = price.toDomainPrice(),
    quantity = quantity,
    sku = sku,
    title = title,
    images = images.map { it.toDomainImage() },
)

fun ProductImageDto.toDomainImage(): ProductImage = ProductImage(
    id = id,
    url = url,
    width = width,
    height = height,
    order = order ?: 0,
)

fun OptionDto.toDomainOption(): Option = Option(
    id = id,
    name = name,
    order = order,
    values = values,
)

fun CategoryDto.toDomainCategory(): Category = Category(id = id, name = name)

fun ProductShippingDto.toDomainProductShipping(): ProductShipping = ProductShipping(
    id = id,
    name = name,
    description = description,
    customPriceEnabled = customPriceEnabled,
    isDefault = defaultOption,
    shippingCountry = shippingCountry?.map { it.toDomainShippingCountry() },
)

fun ShippingCountryDto.toDomainShippingCountry(): ShippingCountry = ShippingCountry(
    id = id,
    country = country,
    price = price.toDomainBasePrice(),
)

fun ReturnInfoDto.toDomainReturnInfo(): ReturnInfo = ReturnInfo(
    returnRight = returnRight,
    returnLabel = returnLabel,
    returnCost = returnCost?.toFloat(),
    supplierPolicy = supplierPolicy,
    returnAddress = returnAddress?.toDomainReturnAddress(),
)

fun ReturnAddressDto.toDomainReturnAddress(): ReturnAddress = ReturnAddress(
    sameAsBusiness = sameAsBusiness,
    sameAsWarehouse = sameAsWarehouse,
    country = country,
    timezone = timezone,
    address = address,
    address2 = address2,
    postCode = postCode,
    returnCity = returnCity,
)

fun MarketDto.toMarket(fallback: io.reachu.VioCore.configuration.MarketConfiguration): Market? {
    if (code.isBlank()) return null
    val marketName = name.ifBlank { fallback.countryName }
    val symbol = currency?.symbol ?: fallback.currencySymbol
    val currencyCode = currency?.code ?: fallback.currencyCode
    val phone = phoneCode ?: fallback.phoneCode
    return Market(
        code = code,
        name = marketName,
        officialName = official,
        flagURL = flag,
        phoneCode = phone,
        currencyCode = currencyCode,
        currencySymbol = symbol,
    )
}

fun CoreProduct.toCartProduct(): Product = Product(
    id = id,
    title = title,
    brand = brand,
    description = description,
    tags = tags,
    sku = sku,
    quantity = quantity,
    price = price.toCartPrice(),
    variants = variants.map { it.toCartVariant() },
    barcode = barcode,
    options = options?.map { it.toCartOption() },
    categories = categories?.map { it.toCartCategory() },
    images = images.map { it.toCartImage() },
    productShipping = productShipping?.map { it.toCartProductShipping() },
    supplier = supplier,
    supplierId = supplierId,
    importedProduct = importedProduct,
    referralFee = referralFee,
    optionsEnabled = optionsEnabled,
    digital = digital,
    origin = origin,
    returnInfo = returns?.toCartReturnInfo(),
)

private fun CorePrice.toCartPrice(): Price = Price(
    amount = amount,
    currencyCode = currencyCode,
    amountInclTaxes = amountInclTaxes,
    taxAmount = taxAmount,
    taxRate = taxRate,
    compareAt = compareAt,
    compareAtInclTaxes = compareAtInclTaxes,
)

private fun CoreVariant.toCartVariant(): Variant = Variant(
    id = id,
    barcode = barcode,
    price = price.toCartPrice(),
    quantity = quantity,
    sku = sku,
    title = title,
    images = images.map { it.toCartImage() },
)

private fun CoreProductImage.toCartImage(): ProductImage = ProductImage(
    id = id,
    url = url,
    width = width,
    height = height,
    order = order,
)

private fun CoreOption.toCartOption(): Option = Option(
    id = id,
    name = name,
    order = order,
    values = values,
)

private fun CoreCategory.toCartCategory(): Category = Category(
    id = id,
    name = name,
)

private fun CoreProductShipping.toCartProductShipping(): ProductShipping = ProductShipping(
    id = id,
    name = name,
    description = description,
    customPriceEnabled = customPriceEnabled,
    isDefault = isDefault,
    shippingCountry = shippingCountry?.map { it.toCartShippingCountry() },
)

private fun CoreShippingCountry.toCartShippingCountry(): ShippingCountry = ShippingCountry(
    id = id,
    country = country,
    price = price.toCartBasePrice(),
)

private fun CoreBasePrice.toCartBasePrice(): BasePrice = BasePrice(
    amount = amount,
    currencyCode = currencyCode,
    amountInclTaxes = amountInclTaxes,
    taxAmount = taxAmount,
    taxRate = taxRate,
)

private fun CoreReturnInfo.toCartReturnInfo(): ReturnInfo = ReturnInfo(
    returnRight = returnRight,
    returnLabel = returnLabel,
    returnCost = returnCost,
    supplierPolicy = supplierPolicy,
    returnAddress = returnAddress?.toCartReturnAddress(),
)

private fun CoreReturnAddress.toCartReturnAddress(): ReturnAddress = ReturnAddress(
    sameAsBusiness = sameAsBusiness,
    sameAsWarehouse = sameAsWarehouse,
    country = country,
    timezone = timezone,
    address = address,
    address2 = address2,
    postCode = postCode,
    returnCity = returnCity,
)

fun Product.toDto(): ProductDto = ProductDto(
    id = id,
    title = title,
    brand = brand,
    description = description,
    tags = tags,
    sku = sku,
    quantity = quantity,
    price = price.toDto(),
    variants = variants.map { it.toDto() },
    barcode = barcode,
    options = options?.map { it.toDto() } ?: emptyList(),
    categories = categories?.map { it.toDto() },
    images = images.map { it.toDto() },
    productShipping = productShipping?.map { it.toDto() },
    supplier = supplier,
    supplierId = supplierId,
    importedProduct = importedProduct,
    referralFee = referralFee,
    optionsEnabled = optionsEnabled,
    digital = digital,
    origin = origin,
    returnInfo = returnInfo?.toDto(),
)

fun Price.toDto(): PriceDto = PriceDto(
    amount = amount.toDouble(),
    currencyCode = currencyCode,
    amountInclTaxes = amountInclTaxes?.toDouble(),
    taxAmount = taxAmount?.toDouble(),
    taxRate = taxRate?.toDouble(),
    compareAt = compareAt?.toDouble(),
    compareAtInclTaxes = compareAtInclTaxes?.toDouble(),
)

fun BasePrice.toDto(): PriceDto = PriceDto(
    amount = amount.toDouble(),
    currencyCode = currencyCode,
    amountInclTaxes = amountInclTaxes?.toDouble(),
    taxAmount = taxAmount?.toDouble(),
    taxRate = taxRate?.toDouble(),
)

fun Variant.toDto(): VariantDto = VariantDto(
    id = id,
    barcode = barcode,
    price = price.toDto(),
    quantity = quantity,
    sku = sku,
    title = title,
    images = images.map { it.toDto() },
)

fun ProductImage.toDto(): ProductImageDto = ProductImageDto(
    id = id,
    url = url,
    width = width,
    height = height,
    order = order,
)

fun Option.toDto(): OptionDto = OptionDto(
    id = id,
    name = name,
    order = order ?: 0,
    values = values ?: "",
)

fun Category.toDto(): CategoryDto = CategoryDto(
    id = id,
    name = name,
)

fun ProductShipping.toDto(): ProductShippingDto = ProductShippingDto(
    id = id,
    name = name,
    description = description,
    customPriceEnabled = customPriceEnabled,
    defaultOption = isDefault,
    shippingCountry = shippingCountry?.map { it.toDto() },
)

fun ShippingCountry.toDto(): ShippingCountryDto = ShippingCountryDto(
    id = id,
    country = country,
    price = price.toDto(),
)

fun ReturnInfo.toDto(): ReturnInfoDto = ReturnInfoDto(
    returnRight = returnRight,
    returnLabel = returnLabel,
    returnCost = returnCost?.toDouble(),
    supplierPolicy = supplierPolicy,
    returnAddress = returnAddress?.toDto(),
)

fun ReturnAddress.toDto(): ReturnAddressDto = ReturnAddressDto(
    sameAsBusiness = sameAsBusiness,
    sameAsWarehouse = sameAsWarehouse,
    country = country,
    timezone = timezone,
    address = address,
    address2 = address2,
    postCode = postCode,
    returnCity = returnCity,
)
