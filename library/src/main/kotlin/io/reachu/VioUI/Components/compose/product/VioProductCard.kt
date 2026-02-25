package io.reachu.VioUI.Components.compose.product

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.configuration.VioLocalization
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioUI.Components.VioProductCardConfig
import io.reachu.VioUI.Components.VioProductCardState
import io.reachu.VioUI.Components.toVioProductCardState
import io.reachu.sdk.domain.models.ProductDto
import io.reachu.VioUI.Managers.Product as DomainProduct
import io.reachu.VioUI.Managers.Variant
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.compose.product.VioHtmlRenderer
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Components.compose.product.VioPlainTextRenderer
import io.reachu.VioUI.Components.compose.utils.toComposeTextStyle
import io.reachu.VioUI.Components.compose.common.SponsorBadgeContainer


private fun String.toColor(): Color = toVioColor()

@Composable
fun VioProductCard(
    product: ProductDto,
    variant: VioProductCardConfig.Variant = VioProductCardConfig.Variant.GRID,
    showBrand: Boolean = true,
    showDescription: Boolean = true,
    showProductDetail: Boolean = true,
    showSponsor: Boolean = false,
    sponsorPosition: String? = "topRight",
    sponsorLogoUrl: String? = null,
    imageBackgroundColor: Color = Color.White,
    onTap: (() -> Unit)? = null,
    onAddToCart: ((Variant?, Int) -> Unit)? = null,
    onShowProductDetail: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
) {
    val config = remember(variant, showBrand, showDescription, showProductDetail) {
        VioProductCardConfig(
            variant = variant,
            showBrand = showBrand,
            showDescription = showDescription,
            showProductDetail = showProductDetail,
        )
    }
    val state = remember(product, config) { product.toVioProductCardState(config) }
    VioProductCard(
        state = state,
        variant = variant,
        showSponsor = showSponsor,
        sponsorPosition = sponsorPosition,
        sponsorLogoUrl = sponsorLogoUrl,
        imageBackgroundColor = imageBackgroundColor,
        onTap = onTap,
        onAddToCart = onAddToCart,
        onShowProductDetail = onShowProductDetail,
        modifier = modifier,
        imageLoader = imageLoader,
    )
}

@Composable
fun VioProductCard(
    product: DomainProduct,
    variant: VioProductCardConfig.Variant = VioProductCardConfig.Variant.GRID,
    showBrand: Boolean = true,
    showDescription: Boolean = true,
    showProductDetail: Boolean = true,
    showSponsor: Boolean = false,
    sponsorPosition: String? = "topRight",
    sponsorLogoUrl: String? = null,
    imageBackgroundColor: Color = Color.White,
    onTap: (() -> Unit)? = null,
    onAddToCart: ((Variant?, Int) -> Unit)? = null,
    onShowProductDetail: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    htmlRenderer: VioHtmlRenderer = VioPlainTextRenderer,
) {
    val config = remember(variant, showBrand, showDescription, showProductDetail) {
        VioProductCardConfig(
            variant = variant,
            showBrand = showBrand,
            showDescription = showDescription,
            showProductDetail = showProductDetail,
        )
    }
    val state = remember(product, config) { product.toVioProductCardState(config) }
    var showDetailSheet by remember { mutableStateOf(false) }
    val autoDetailHandler = remember(showProductDetail, onTap, onShowProductDetail) {
        if (showProductDetail && onTap == null && onShowProductDetail == null) {
            {
                AnalyticsManager.trackProductViewed(
                    productId = product.id.toString(),
                    productName = product.title,
                    productPrice = product.price.amount.toDouble(),
                    productCurrency = product.price.currencyCode,
                    source = "product_store",
                )
                showDetailSheet = true
            }
        } else null
    }
    val variants = remember(product) { product.variants }
    val onAddToCartWithVariantCheck = remember(product, onAddToCart, variants) {
        if (onAddToCart == null) {
            null
        } else if (variants.size > 1) {
            { _: Variant?, _: Int ->
                AnalyticsManager.trackProductViewed(
                    productId = product.id.toString(),
                    productName = product.title,
                    productPrice = product.price.amount.toDouble(),
                    productCurrency = product.price.currencyCode,
                    source = "product_store_variant_redirect",
                )
                showDetailSheet = true
            }
        } else {
            onAddToCart
        }
    }

    VioProductCard(
        state = state,
        variant = variant,
        showSponsor = showSponsor,
        sponsorPosition = sponsorPosition,
        sponsorLogoUrl = sponsorLogoUrl,
        imageBackgroundColor = imageBackgroundColor,
        onTap = if (autoDetailHandler != null) null else onTap,
        onAddToCart = onAddToCartWithVariantCheck,
        onShowProductDetail = onShowProductDetail ?: autoDetailHandler,
        modifier = modifier,
        imageLoader = imageLoader,
    )
    if (showDetailSheet) {
        VioProductDetailOverlay(
            product = product,
            currencySymbol = config.currencySymbol.ifBlank { product.price.currencyCode },
            onAddToCart = { variant, quantity ->
                onAddToCart?.invoke(variant, quantity)
            },
            onDismiss = { showDetailSheet = false },
            imageLoader = imageLoader,
            htmlRenderer = htmlRenderer,
        )
    }
}

@Composable
fun VioProductCard(
    state: VioProductCardState,
    variant: VioProductCardConfig.Variant = VioProductCardConfig.Variant.GRID,
    showSponsor: Boolean = false,
    sponsorPosition: String? = "topRight",
    sponsorLogoUrl: String? = null,
    imageBackgroundColor: Color = Color.White,
    onTap: (() -> Unit)? = null,
    onAddToCart: ((Variant?, Int) -> Unit)? = null,
    onShowProductDetail: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
) {
    val tapHandler = when {
        state.showProductDetail && onShowProductDetail != null -> onShowProductDetail
        else -> onTap
    }
    val clickableModifier = if (tapHandler != null) {
        modifier.clickable { tapHandler() }
    } else {
        modifier
    }
    when (variant) {
        VioProductCardConfig.Variant.GRID -> GridCard(state, clickableModifier, imageLoader, onAddToCart, showSponsor, sponsorPosition, sponsorLogoUrl, imageBackgroundColor)
        VioProductCardConfig.Variant.LIST -> ListCard(state, clickableModifier, imageLoader, onAddToCart, showSponsor, sponsorPosition, sponsorLogoUrl, imageBackgroundColor)
        VioProductCardConfig.Variant.HERO -> HeroCard(state, clickableModifier, onAddToCart, imageLoader, showSponsor, sponsorPosition, sponsorLogoUrl, imageBackgroundColor)
        VioProductCardConfig.Variant.MINIMAL -> MinimalCard(state, clickableModifier, imageLoader, showSponsor, sponsorPosition, sponsorLogoUrl, imageBackgroundColor)
    }
}

@Composable
private fun GridCard(
    state: VioProductCardState,
    modifier: Modifier,
    imageLoader: VioImageLoader,
    onAddToCart: ((Variant?, Int) -> Unit)?,
    showSponsor: Boolean,
    sponsorPosition: String?,
    sponsorLogoUrl: String?,
    imageBackgroundColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VioColors.surface.toColor()),
        shape = RoundedCornerShape(VioBorderRadius.large.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        SponsorBadgeContainer(
            showSponsor = showSponsor,
            sponsorPosition = sponsorPosition,
            sponsorLogoUrl = sponsorLogoUrl,
            imageLoader = imageLoader,
        ) {
            Column(modifier = Modifier.padding(VioSpacing.md.dp)) {
                Box {
                    ImageGallery(state, height = 160.dp, imageLoader = imageLoader, backgroundColor = imageBackgroundColor)
                    state.discountPercentage?.let {
                        DiscountBadge(
                            text = "-${it}%",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(VioSpacing.sm.dp))
                if (state.canShowBrand) {
                    Text(
                        state.brand.orEmpty(),
                        style = VioTypography.caption1.toComposeTextStyle(),
                        color = VioColors.textSecondary.toColor(),
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    state.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
                    color = VioColors.textPrimary.toColor(),
                )
                if (state.canShowDescription) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        state.description.orEmpty(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = VioColors.textSecondary.toColor(),
                    )
                }
                Spacer(Modifier.height(VioSpacing.xs.dp))
                PriceBlock(state)
                Spacer(Modifier.height(VioSpacing.sm.dp))
                when {
                    !state.isInStock -> OutOfStockLabel()
                    onAddToCart != null -> AddToCartButton(onAddToCart = { variant, quantity -> onAddToCart(variant, quantity) })
                }
            }
        }
    }
}

@Composable
private fun ListCard(
    state: VioProductCardState,
    modifier: Modifier,
    imageLoader: VioImageLoader,
    onAddToCart: ((Variant?, Int) -> Unit)?,
    showSponsor: Boolean,
    sponsorPosition: String?,
    sponsorLogoUrl: String?,
    imageBackgroundColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VioColors.surface.toColor()),
        shape = RoundedCornerShape(VioBorderRadius.small.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        SponsorBadgeContainer(
            showSponsor = showSponsor,
            sponsorPosition = sponsorPosition,
            sponsorLogoUrl = sponsorLogoUrl,
            imageLoader = imageLoader,
        ) {
            Row(
                modifier = Modifier.padding(VioSpacing.sm.dp),
                horizontalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    SingleImage(state, width = 70.dp, height = 70.dp, imageLoader = imageLoader, backgroundColor = imageBackgroundColor)
                    state.discountPercentage?.let {
                        DiscountBadge(
                            text = "-${it}%",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (state.canShowBrand) {
                        Text(
                            state.brand.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = VioColors.textSecondary.toColor(),
                            maxLines = 1,
                        )
                    }
                    Text(
                        state.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
                        color = VioColors.textPrimary.toColor(),
                    )
                    if (state.canShowDescription) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            state.description.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = VioColors.textSecondary.toColor(),
                        )
                    }
                    Spacer(Modifier.height(VioSpacing.xs.dp))
                    PriceBlock(state)
                    Spacer(Modifier.height(VioSpacing.xs.dp))
                    when {
                        !state.isInStock -> OutOfStockLabel()
                        onAddToCart != null -> AddToCartButton(onAddToCart = { variant, quantity -> onAddToCart(variant, quantity) }, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    state: VioProductCardState,
    modifier: Modifier,
    onAddToCart: ((Variant?, Int) -> Unit)?,
    imageLoader: VioImageLoader,
    showSponsor: Boolean,
    sponsorPosition: String?,
    sponsorLogoUrl: String?,
    imageBackgroundColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VioColors.surface.toColor()),
        shape = RoundedCornerShape(VioBorderRadius.large.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        SponsorBadgeContainer(
            showSponsor = showSponsor,
            sponsorPosition = sponsorPosition,
            sponsorLogoUrl = sponsorLogoUrl,
            imageLoader = imageLoader,
        ) {
            Column(modifier = Modifier.padding(VioSpacing.lg.dp)) {
                Box {
                    ImageGallery(state, height = 300.dp, imageLoader = imageLoader, backgroundColor = imageBackgroundColor)
                    state.discountPercentage?.let {
                        DiscountBadge(
                            text = "-${it}%",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(VioSpacing.sm.dp))
                state.brand?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = VioColors.textSecondary.toColor())
                }
                Text(
                    state.title,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = VioColors.textPrimary.toColor()
                )
                state.description?.let {
                    Spacer(Modifier.height(VioSpacing.sm.dp))
                    Text(
                        it,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = VioColors.textSecondary.toColor()
                    )
                }
                Spacer(Modifier.height(VioSpacing.sm.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceBlock(state)
                    Spacer(Modifier.weight(1f))
                    when {
                        !state.isInStock -> OutOfStockLabel()
                        onAddToCart != null -> {
                            val label = VioLocalization.string("common.addToCart", defaultValue = "Add to Cart")
                            Button(
                                onClick = { onAddToCart(null, 1) },
                                enabled = state.isInStock,
                                colors = ButtonDefaults.buttonColors(containerColor = VioColors.primary.toColor()),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalCard(
    state: VioProductCardState,
    modifier: Modifier,
    imageLoader: VioImageLoader,
    showSponsor: Boolean,
    sponsorPosition: String?,
    sponsorLogoUrl: String?,
    imageBackgroundColor: Color,
) {
    Card(
        modifier = modifier.size(width = 100.dp, height = 140.dp),
        colors = CardDefaults.cardColors(containerColor = VioColors.surface.toColor()),
        shape = RoundedCornerShape(VioBorderRadius.small.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        SponsorBadgeContainer(
            showSponsor = showSponsor,
            sponsorPosition = sponsorPosition,
            sponsorLogoUrl = sponsorLogoUrl,
            imageLoader = imageLoader,
        ) {
            Column(modifier = Modifier.padding(VioSpacing.xs.dp)) {
                SingleImage(state, width = 100.dp, height = 80.dp, imageLoader = imageLoader, backgroundColor = imageBackgroundColor)
                Spacer(Modifier.height(2.dp))
                Text(
                    state.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.merge(TextStyle(fontWeight = FontWeight.Medium)),
                    color = VioColors.textPrimary.toColor()
                )
                Spacer(Modifier.height(2.dp))
                PriceBlock(state)
            }
        }
    }
}

@Composable
private fun ImageGallery(
    state: VioProductCardState,
    height: androidx.compose.ui.unit.Dp,
    imageLoader: VioImageLoader,
    backgroundColor: Color = Color.White,
) {
    val gallery = state.imageGallery
    if (gallery.isEmpty()) {
        SingleImage(state, width = null, height = height, imageLoader = imageLoader, backgroundColor = backgroundColor)
        return
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { gallery.size })
    Column {
        HorizontalPager(state = pagerState, modifier = Modifier.height(height)) { page ->
            Box(modifier = Modifier
                .fillMaxHeight()
                .background(backgroundColor)
                .clip(RoundedCornerShape(VioBorderRadius.medium.dp))
            ) {
                VioImage(
                    url = gallery[page],
                    contentDescription = state.title,
                    modifier = Modifier.fillMaxHeight(),
                    imageLoader = imageLoader,
                )
            }
        }
        if (gallery.size > 1) {
            Spacer(Modifier.height(VioSpacing.xs.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(gallery.size.coerceAtMost(5)) { index ->
                    val selected = index == pagerState.currentPage
                    Canvas(modifier = Modifier.size(if (selected) 8.dp else 6.dp)) {
                        drawCircle(color = if (selected) VioColors.primary.toColor() else Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleImage(
    state: VioProductCardState,
    width: androidx.compose.ui.unit.Dp?,
    height: androidx.compose.ui.unit.Dp,
    imageLoader: VioImageLoader,
    backgroundColor: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(backgroundColor)
            .clip(RoundedCornerShape(VioBorderRadius.medium.dp))
    ) {
        VioImage(
            url = state.primaryImage,
            contentDescription = state.title,
            modifier = Modifier
                .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
                .height(height),
            imageLoader = imageLoader,
        )
    }
}

@Composable
private fun PriceBlock(state: VioProductCardState) {
    Column {
        Text(
            state.priceLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = VioColors.primary.toColor()
        )
        state.compareAtLabel?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall.merge(TextStyle(textDecoration = TextDecoration.LineThrough)),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddToCartButton(onAddToCart: (Variant?, Int) -> Unit, compact: Boolean = false) {
    val label = VioLocalization.string("common.addToCart", defaultValue = "Add to Cart")
    Button(
        onClick = { onAddToCart(null, 1) },
        modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VioBorderRadius.medium.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VioColors.primary.toColor(),
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun OutOfStockLabel() {
    val text = VioLocalization.string("product.outOfStock", defaultValue = "Out of Stock")
    Text(
        text = text,
        style = VioTypography.caption1.toComposeTextStyle(),
        color = VioColors.error.toColor(),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = VioColors.error.toColor(),
                shape = RoundedCornerShape(VioBorderRadius.small.dp),
            )
            .padding(horizontal = VioSpacing.sm.dp, vertical = VioSpacing.xs.dp),
    )
}

@Composable
private fun DiscountBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
        modifier = modifier
            .background(
                color = VioColors.primary.toColor(),
                shape = RoundedCornerShape(VioBorderRadius.pill.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
