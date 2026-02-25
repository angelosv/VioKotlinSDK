package com.reachu.tv2demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.reachu.VioUI.Components.compose.product.VioProductSlider
import io.reachu.VioUI.Components.compose.product.VioProductBanner
import io.reachu.VioUI.Components.compose.product.VioProductStore
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import com.reachu.tv2demo.casting.CastUiState
import com.reachu.tv2demo.ui.components.CategoryChip
import com.reachu.tv2demo.ui.components.ContentCard
import com.reachu.tv2demo.ui.components.BottomTabBar
import com.reachu.tv2demo.ui.components.TV2OfferBanner
import com.reachu.tv2demo.ui.model.ContentMockData
import com.reachu.tv2demo.ui.model.ContentItem
import com.reachu.tv2demo.ui.model.Category
import com.reachu.tv2demo.ui.model.TabItem
import com.reachu.tv2demo.ui.theme.TV2Theme
import io.reachu.VioUI.Components.VioProductSliderLayout
import io.reachu.VioUI.adapters.VioCoilImageLoader
import io.reachu.VioUI.Managers.loadProductsIfNeeded

@Composable
fun TV2HomeScreen(
    cartManager: CartManager,
    castingState: CastUiState,
    onShowCasting: () -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onOpenCheckout: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(ContentMockData.categories.first()) }
    var filteredContent by remember { mutableStateOf(ContentMockData.items) }
    var selectedTab by remember { mutableStateOf(TabItem.HOME) }

    LaunchedEffect(selectedCategory) {
        filteredContent = ContentMockData.items.filter { it.category == selectedCategory.name || selectedCategory.slug == "sporten" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TV2Theme.Colors.background),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val scroll = rememberScrollState()
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                TabItem.HOME -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll),
                    ) {
                        TopBar(castingState = castingState, onCastingClick = onShowCasting)
                        CategoryRow(selectedCategory = selectedCategory, onCategorySelected = { selectedCategory = it })
                        ContentSection(title = "Direkte", items = filteredContent.filter { it.isLive })
                        ContentSection(title = "Nylig", items = filteredContent.filter { !it.isLive })
                        Spacer(Modifier.height(TV2Theme.Spacing.lg))
                        OffersSection(cartManager, onOpenCheckout)
                        Spacer(Modifier.height(TV2Theme.Spacing.lg))
                        ProductsSection(cartManager, onOpenProductDetail)
                        Spacer(Modifier.height(TV2Theme.Spacing.xl))
                    }
                }
                TabItem.STORE -> {
                    StoreTabContent(cartManager = cartManager)
                }
                else -> {
                    // Placeholder for other tabs
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(selectedTab.label, style = TV2Theme.Typography.title, color = TV2Theme.Colors.textSecondary)
                    }
                }
            }
        }
        BottomTabBar(
            selected = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TopBar(castingState: CastUiState, onCastingClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TV2Theme.Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("TV2 Demo", style = TV2Theme.Typography.largeTitle, color = TV2Theme.Colors.textPrimary)
            Text("Direkte + Shopping", style = TV2Theme.Typography.body, color = TV2Theme.Colors.textSecondary)
        }
        Button(onClick = onCastingClick) {
            Text(if (castingState.isCasting) "Åpne casting" else "Cast til TV")
        }
    }
}

@Composable
private fun CategoryRow(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = TV2Theme.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(TV2Theme.Spacing.sm),
    ) {
        ContentMockData.categories.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun ContentSection(title: String, items: List<ContentItem>) {
    if (items.isEmpty()) return
    Column(
        modifier = Modifier.padding(top = TV2Theme.Spacing.lg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TV2Theme.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = TV2Theme.Typography.title, color = TV2Theme.Colors.textPrimary)
            Text("Se alle", style = TV2Theme.Typography.caption, color = TV2Theme.Colors.textSecondary)
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = TV2Theme.Spacing.md, vertical = TV2Theme.Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(TV2Theme.Spacing.md),
        ) {
            items.forEach { item ->
                ContentCard(item = item)
            }
        }
    }
}

@Composable
private fun OffersSection(cartManager: CartManager, onOpenCheckout: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = TV2Theme.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(TV2Theme.Spacing.md),
    ) {
        TV2OfferBanner(onClick = onOpenCheckout)
        VioProductBanner(
            modifier = Modifier.fillMaxWidth(),
            isCampaignGated = false,
            showSponsor = true,
            sponsorPosition = "top_right",
        )
    }
}

@Composable
private fun ProductsSection(
    cartManager: CartManager,
    onOpenProductDetail: (Product) -> Unit,
) {
    // Asegurar que los productos (y sus imágenes) se carguen al mostrar la sección.
    // En DemoApp esto se dispara desde MarketManager/Checkout; aquí lo hacemos explícito.
    LaunchedEffect(Unit) {
        cartManager.loadProductsIfNeeded()
    }
    Column(
        modifier = Modifier.padding(horizontal = TV2Theme.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(TV2Theme.Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ukens tilbud", style = TV2Theme.Typography.title, color = TV2Theme.Colors.textPrimary)
            AsyncImage(
                model = "https://static.vecteezy.com/system/resources/previews/020/336/912/original/tv-2-logo-tv-television-brand-white-symbol-design-norway-channel-free-vector.jpg",
                contentDescription = "TV2",
                modifier = Modifier.size(32.dp),
            )
        }
        VioProductSlider(
                            cartManager = cartManager,
                title = "Featured Products",
                layout = VioProductSliderLayout.CARDS,
                showSeeAll = true,
            onProductTap = { product -> onOpenProductDetail(product) },
                currency = cartManager.currency,
                country = cartManager.country,
                imageLoader = VioCoilImageLoader,
                isCampaignGated = false,
                products = cartManager.products,
                isLoading = cartManager.isProductsLoading,
                showSponsor = true,
                sponsorPosition = "top_right",      

        )
        VioProductStore(
            cartManager = cartManager,
            modifier = Modifier.fillMaxWidth(),
            showSponsor = true,
            sponsorPosition = "top_right", 
            isCampaignGated = true,
            isScrollEnabled = false,
        )
    }
}
@Composable
private fun StoreTabContent(cartManager: CartManager) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TV2Theme.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Butikk",
                style = TV2Theme.Typography.largeTitle,
                color = TV2Theme.Colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            AsyncImage(
                model = "https://static.vecteezy.com/system/resources/previews/020/336/912/original/tv-2-logo-tv-television-brand-white-symbol-design-norway-channel-free-vector.jpg",
                contentDescription = "TV2",
                modifier = Modifier.size(32.dp),
            )
        }
        
        VioProductStore(
            cartManager = cartManager,
            modifier = Modifier.fillMaxWidth(),
            isScrollEnabled = false, // Handled by outer scroll
            isCampaignGated = false,
            showSponsor = true,
            sponsorPosition = "top_right",   
        )
        
        Spacer(Modifier.height(80.dp)) // Padding for bottom bar
    }
}
