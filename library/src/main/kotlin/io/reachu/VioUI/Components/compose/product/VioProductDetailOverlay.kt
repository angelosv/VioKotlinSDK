package io.reachu.VioUI.Components.compose.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.configuration.CloseButtonStyle
import io.reachu.VioCore.configuration.ProductDetailConfiguration
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioUI.Components.VioProductCardConfig
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.toVioProductCardState
import io.reachu.VioUI.Managers.Option
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Managers.ProductImage
import io.reachu.VioUI.Managers.Variant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import android.text.method.LinkMovementMethod
import androidx.compose.ui.graphics.toArgb

private fun String.toColor(): Color = toVioColor()

fun interface VioHtmlRenderer {
    @Composable
    fun Render(html: String, modifier: Modifier)
}

object VioPlainTextRenderer : VioHtmlRenderer {
    @Composable
    override fun Render(html: String, modifier: Modifier) {
        Text(
            text = html,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = VioColors.textSecondary.toColor(),
        )
    }
}

object VioAndroidHtmlRenderer : VioHtmlRenderer {
    @Composable
    override fun Render(html: String, modifier: Modifier) {
        val textColor = VioColors.textSecondary.toColor().toArgb()
        AndroidView(
            modifier = modifier,
            factory = { context ->
                TextView(context).apply {
                    textSize = 14f
                    setTextColor(textColor)
                    movementMethod = LinkMovementMethod.getInstance()
                }
            },
            update = { textView ->
                textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VioProductDetailOverlay(
    product: Product,
    currencySymbol: String,
    onAddToCart: (variant: Variant?, quantity: Int) -> Unit,
    onDismiss: () -> Unit,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    htmlRenderer: VioHtmlRenderer = VioAndroidHtmlRenderer,
    isCampaignGated: Boolean = true,
) {
    val campaignManager = remember { CampaignManager.shared }
    val campaignActive by campaignManager.isCampaignActive.collectAsState(initial = true)
    val currentCampaign by campaignManager.currentCampaign.collectAsState(initial = null)
    
    val shouldShow = remember(isCampaignGated, campaignActive, currentCampaign) {
        if (!isCampaignGated) return@remember true
        if (!VioConfiguration.shared.shouldUseSDK) return@remember false
        campaignActive && currentCampaign?.isPaused != true
    }


    if (!shouldShow) return

    val detailConfig = VioConfiguration.shared.state.value.productDetail
    val scope = rememberCoroutineScope()
    val variants = remember(product) { product.variants }
    val sortedOptions = remember(product) { product.options?.sortedBy { it.order } ?: emptyList() }
    val optionValues = remember(sortedOptions, variants) { buildOptionValueMap(sortedOptions, variants) }
    var selectedOptions by remember(product) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedVariant by remember(product) { mutableStateOf(variants.firstOrNull()) }
    var quantity by remember { mutableIntStateOf(1) }
    LaunchedEffect(product) {
        val defaults = initialOptionSelection(sortedOptions, variants.firstOrNull())
        selectedOptions = defaults
        selectedVariant = resolveVariant(variants, sortedOptions, defaults)
        quantity = 1
    }

    val images = remember(product) { product.images.sortedBy { it.order } }
    val pagerState = rememberPagerState(pageCount = { max(1, images.size) })
    var isAdding by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val displayCurrency = remember(currencySymbol, product.price.currencyCode) {
        product.price.currencyCode.ifBlank { currencySymbol }
    }
    val currentPriceValue = remember(selectedVariant, product) {
        (selectedVariant?.price?.amountInclTaxes
            ?: selectedVariant?.price?.amount
            ?: product.price.amountInclTaxes
            ?: product.price.amount).toDouble()
    }
    val compareAtValue = remember(selectedVariant, product) {
        (selectedVariant?.price?.compareAtInclTaxes
            ?: selectedVariant?.price?.compareAt
            ?: product.price.compareAtInclTaxes
            ?: product.price.compareAt)?.toDouble()
    }
    val isInStock = remember(selectedVariant, product) {
        (selectedVariant?.quantity ?: product.quantity ?: 0) > 0
    }
    val maxQuantity = max(1, selectedVariant?.quantity ?: product.quantity ?: 1)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (detailConfig.showCloseButton && detailConfig.closeButtonStyle != CloseButtonStyle.NAVIGATION_BAR) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(
                                if (detailConfig.closeButtonStyle == CloseButtonStyle.OVERLAY_TOP_LEFT) Alignment.TopStart else Alignment.TopEnd,
                            ),
                        ) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    }
                }
                ImageGallery(
                    images = images,
                    pagerState = pagerState,
                    detailConfig = detailConfig,
                    imageLoader = imageLoader,
                )
                Column(modifier = Modifier.padding(horizontal = VioSpacing.lg.dp)) {
                    Text(
                        product.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = VioColors.textPrimary.toColor(),
                    )
                    if (!product.brand.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            product.brand,
                            style = MaterialTheme.typography.labelSmall,
                            color = VioColors.textSecondary.toColor(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    PriceRow(
                        price = formatPrice(displayCurrency, currentPriceValue),
                        compareAt = compareAtValue?.let { formatPrice(displayCurrency, it) },
                    )
                    Spacer(Modifier.height(8.dp))
                    StockBadge(isInStock = isInStock)
                }
                if (optionValues.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    OptionSelector(
                        optionValues = optionValues,
                        selected = selectedOptions,
                        onSelected = { option, value ->
                            val updated = selectedOptions.toMutableMap()
                            updated[option] = value
                            selectedOptions = updated
                            selectedVariant = resolveVariant(variants, sortedOptions, updated)
                            quantity = 1
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                QuantitySelector(
                    quantity = quantity,
                    maxQuantity = maxQuantity,
                    onIncrement = { if (quantity < maxQuantity) quantity++ },
                    onDecrement = { if (quantity > 1) quantity-- },
                )
                if (detailConfig.showDescription && !product.description.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = VioSpacing.lg.dp)) {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = VioColors.textPrimary.toColor(),
                        )
                        Spacer(Modifier.height(8.dp))
                        htmlRenderer.Render(product.description.orEmpty(), modifier = Modifier.fillMaxWidth())
                    }
                }
                if (detailConfig.showSpecifications) {
                    Spacer(Modifier.height(16.dp))
                    SpecificationsSection(product = product, selectedVariant = selectedVariant)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (!isAdding && isInStock) {
                            isAdding = true
                            scope.launch {
                                onAddToCart(selectedVariant, quantity)
                                isAdding = false
                                showSuccess = true
                                delay(600)
                                showSuccess = false
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VioSpacing.lg.dp),
                    enabled = isInStock && !isAdding,
                ) {
                    Text(
                        if (isAdding) "Processing..." else "Add to Cart • ${formatPrice(displayCurrency, currentPriceValue * quantity)}",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (showSuccess) {
                SuccessOverlay(productTitle = product.title, quantity = quantity)
            }
        }
    }

}

@Composable
private fun PriceRow(price: String, compareAt: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = VioSpacing.lg.dp),
    ) {
        Text(
            price,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = VioColors.primary.toColor(),
        )
        if (compareAt != null) {
            Text(
                compareAt,
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.textSecondary.toColor(),
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
private fun StockBadge(isInStock: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isInStock) VioColors.success.toColor() else VioColors.error.toColor()),
        )
        Text(
            if (isInStock) "In Stock" else "Out of Stock",
            style = MaterialTheme.typography.bodySmall,
            color = if (isInStock) VioColors.success.toColor() else VioColors.error.toColor(),
        )
    }
}

@Composable
private fun OptionSelector(
    optionValues: Map<String, List<String>>,
    selected: Map<String, String>,
    onSelected: (String, String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = VioSpacing.lg.dp)) {
        optionValues.forEach { (name, values) ->
            Text(name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEach { value ->
                    val isSelected = selected[name] == value
                    Text(
                        text = value,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isSelected) VioColors.primary.toColor() else VioColors.border.toColor(),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onSelected(name, value) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (isSelected) VioColors.primary.toColor() else VioColors.textPrimary.toColor(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    maxQuantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VioSpacing.lg.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Quantity", style = MaterialTheme.typography.titleMedium, color = VioColors.textPrimary.toColor())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = quantity > 1,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
            Text("$quantity", style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = onIncrement,
                enabled = quantity < maxQuantity,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
        }
    }
    Spacer(Modifier.height(8.dp))
    Divider(
        color = VioColors.border.toColor(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VioSpacing.lg.dp),
    )
}

@Composable
private fun SpecificationsSection(product: Product, selectedVariant: Variant?) {
    val specs = buildList {
        if (product.sku.isNotBlank()) add("SKU" to product.sku)
        val categories = product.categories?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
        if (categories.isNotEmpty()) add("Category" to categories.joinToString(", "))
        if (product.supplier.isNotBlank()) add("Supplier" to product.supplier)
        add("Stock" to "${selectedVariant?.quantity ?: product.quantity ?: 0} available")
    }
    if (specs.isEmpty()) return
    Column(modifier = Modifier.padding(horizontal = VioSpacing.lg.dp)) {
        Text("Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(8.dp))
        specs.forEach { (title, value) ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = VioColors.textSecondary.toColor())
                Text(value, style = MaterialTheme.typography.bodySmall, color = VioColors.textPrimary.toColor())
            }
        }
    }
}

@Composable
private fun SuccessOverlay(productTitle: String, quantity: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Added to cart", style = MaterialTheme.typography.titleMedium, color = VioColors.textPrimary.toColor())
            Text("$quantity × $productTitle", style = MaterialTheme.typography.bodyMedium, color = VioColors.textSecondary.toColor())
        }
    }
}

@Composable
private fun ImageGallery(
    images: List<ProductImage>,
    pagerState: PagerState,
    detailConfig: ProductDetailConfiguration,
    imageLoader: VioImageLoader,
) {
    if (images.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(detailConfig.imageHeight?.dp ?: 320.dp)
                .padding(horizontal = if (detailConfig.imageFullWidth) 0.dp else VioSpacing.lg.dp)
                .clip(RoundedCornerShape(detailConfig.imageCornerRadius.dp))
                .background(VioColors.surface.toColor()),
            contentAlignment = Alignment.Center,
        ) {
            Text("No image available", color = VioColors.textSecondary.toColor())
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(detailConfig.imageHeight?.dp ?: 320.dp)
                .padding(horizontal = if (detailConfig.imageFullWidth) 0.dp else VioSpacing.lg.dp),
        ) { page ->
            val image = images.getOrNull(page)
            VioImage(
                url = image?.url,
                contentDescription = image?.id,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(detailConfig.imageCornerRadius.dp)),
                imageLoader = imageLoader,
            )
        }
        if (detailConfig.showImageGallery && images.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = VioSpacing.lg.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                images.forEachIndexed { index, img ->
                    VioImage(
                        url = img.url,
                        contentDescription = img.id,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (pagerState.currentPage == index) 2.dp else 1.dp,
                                color = if (pagerState.currentPage == index) VioColors.primary.toColor() else VioColors.border.toColor(),
                                shape = RoundedCornerShape(12.dp),
                            ),
                        imageLoader = imageLoader,
                    )
                }
            }
        }
    }
}

private fun formatPrice(symbol: String, amount: Double): String {
    val sep = if (symbol.length > 1) " " else ""
    return "%s%s%.2f".format(symbol, sep, amount)
}

private fun buildOptionValueMap(
    options: List<Option>,
    variants: List<Variant>,
): Map<String, List<String>> {
    if (options.isEmpty()) return emptyMap()
    val values = options.associate { it.name to mutableSetOf<String>() }
    variants.forEach { variant ->
        val components = parseVariantComponents(variant.title)
        options.forEachIndexed { index, option ->
            if (index < components.size) {
                values[option.name]?.add(components[index].trim())
            }
        }
    }
    return options.associate { option ->
        option.name to values[option.name]?.map { it }?.sorted().orEmpty()
    }
}

private fun initialOptionSelection(
    options: List<Option>,
    defaultVariant: Variant?,
): Map<String, String> {
    if (options.isEmpty() || defaultVariant == null) return emptyMap()
    val components = parseVariantComponents(defaultVariant.title)
    val map = mutableMapOf<String, String>()
    options.forEachIndexed { index, option ->
        if (index < components.size) {
            map[option.name] = components[index].trim()
        }
    }
    return map
}

private fun resolveVariant(
    variants: List<Variant>,
    options: List<Option>,
    selected: Map<String, String>,
): Variant? {
    if (options.isEmpty()) return variants.firstOrNull()
    return variants.firstOrNull { variant ->
        val components = parseVariantComponents(variant.title)
        options.allIndexed { index, option ->
            if (index < components.size) {
                val expected = components[index].trim()
                selected[option.name]?.equals(expected, ignoreCase = false) == true
            } else false
        }
    }
}

private fun parseVariantComponents(title: String): List<String> {
    val dashSplit = title.split(" - ")
    if (dashSplit.size > 1) return dashSplit
    return title.split("-")
}

private inline fun <T> List<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
    forEachIndexed { index, element ->
        if (!predicate(index, element)) return false
    }
    return true
}
