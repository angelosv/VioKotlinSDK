package com.reachu.viaplaydemo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reachu.viaplaydemo.casting.CastUiState
import com.reachu.viaplaydemo.ui.components.BottomTabBar
import com.reachu.viaplaydemo.ui.model.TabItem
import io.reachu.VioUI.Components.compose.product.VioProductSlider
import io.reachu.VioUI.Components.VioProductSliderLayout
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.adapters.VioCoilImageLoader
import io.reachu.VioUI.Managers.loadProductsIfNeeded
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale

data class ContentItem(val title: String, val subtitle: String?, val imageUrl: String?)

@Composable
fun ViaplayHomeView(
    cartManager: CartManager,
    castingState: CastUiState,
    onShowCasting: () -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onOpenCheckout: () -> Unit,
    onOpenSport: () -> Unit,
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B25))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Hero Section Mock
            HeroSectionMock()
            
            // Pagination dots
            // Row(
                // modifier = Modifier
                    // .fillMaxWidth()
                    // .padding(vertical = 16.dp),
                // horizontalArrangement = Arrangement.Center,
                // verticalAlignment = Alignment.CenterVertically
            // ) {
                // Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                // Spacer(modifier = Modifier.width(5.dp))
                // repeat(4) {
                    // Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                    // Spacer(modifier = Modifier.width(5.dp))
                // }
            // }

            // Categories Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryButton("Series", modifier = Modifier.weight(1f))
                    CategoryButton("Films", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryButton("Sport", modifier = Modifier.weight(1f).clickable { onOpenSport() })
                    CategoryButton("Kids", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryButton("Channels", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Akkurat nå ser andre på
            HorizontalContentSection(
                title = "Akkurat nå ser andre på",
                items = listOf(
                    ContentItem("Paradise Hotel", "S3 | E2", "https://i-viaplay-com.akamaized.net/viaplay-prod/468/948/1765544051-b83d0f2dcad01079f7822621644f30cd6ab13bdf.jpg?width=400&height=600"),
                    ContentItem("Mødre", "S1 | E2", "https://i-viaplay-com.akamaized.net/viaplay-prod/867/288/1769507332-592292aa4007214b107153e49315d1f2ccfcbf58.jpg?width=400&height=600"),
                    ContentItem("Spenningens helter", "S2 | E13", "https://i-viaplay-com.akamaized.net/viaplay-prod/609/568/1770635338-c0d013b0b87f8e308a711cb85b996933b80b6c45.jpg?width=400&height=600")
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Nytt hos oss
            HorizontalContentSection(
                title = "Nytt hos oss",
                items = listOf(
                    ContentItem("Rust", null, "https://i-viaplay-com.akamaized.net/viaplay-prod/69/896/1752825320-b87c04eb48f56543b8e31dd2be5466dfd92ebc5c_NO.jpg?width=400&height=600"),
                    ContentItem("Karate Kid Legend", null, "https://i-viaplay-com.akamaized.net/viaplay-prod/93/820/1751982156-688bc4894fa5a1db4d366c8acdc51d34c5315560_NO.jpg?width=400&height=600"),
                    ContentItem("Den skyldige", null, "https://i-viaplay-com.akamaized.net/viaplay-prod/81/392/1539700205-63bd1a3cef4f4d656d2b62cbfeb03d1a38486fc3_NO.jpg?width=400&height=600")
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Ukens tilbud (Reachu SDK)
            Text(
                "Ukens tilbud",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            LaunchedEffect(Unit) {
                cartManager.loadProductsIfNeeded()
            }
            
            VioProductSlider(
                cartManager = cartManager,
                title = "",
                layout = VioProductSliderLayout.CARDS,
                showSeeAll = false,
                onProductTap = { product -> onOpenProductDetail(product) },
                currency = cartManager.currency,
                country = cartManager.country,
                imageLoader = VioCoilImageLoader,
                isCampaignGated = false,
                products = cartManager.products,
                isLoading = cartManager.isProductsLoading,
                showSponsor = true,
                sponsorPosition = "topRight"
            )

            Spacer(modifier = Modifier.height(100.dp)) // Nav bar padding
        }

        // Floating Header
        AnimatedVisibility(
            visible = scrollState.value > 200,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1B1B25).copy(alpha = 0.85f))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(28.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.reachu.viaplaydemo.R.drawable.logo_red),
                            contentDescription = "Viaplay",
                            modifier = Modifier.height(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Cyan.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(16.dp))
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        }

        // Bottom Navigation
        BottomTabBar(
            selected = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }
}

@Composable
fun HeroSectionMock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(Color(0xFF1B1B25))
    ) {
        // Background Image
        AsyncImage(
            model = "https://i-viaplay-com.akamaized.net/viaplay-prod/468/948/1765544025-72ed525cf2272523585d6c5eaabe0221676f1361.png?width=1440",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay for better text contrast if needed (subtle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                        startX = 0f,
                        endX = 1000f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Content: Logo and Text
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(end = 16.dp)
            ) {
                // Viaplay Header Style Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = com.reachu.viaplaydemo.R.drawable.logo_white),
                        contentDescription = "Viaplay",
                        modifier = Modifier
                            .padding(start = 22.dp)
                            .height(40.dp)
                            .scale(1.8f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Ubegrenset\nunderholdning",
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.zIndex(10f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Stream filmer, serier,\n barnas favoritter,\n sport og mer.",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Avslutt når du vil",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // Right Content: Overlapping Posters
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left poster (lowest)
                AsyncImage(
                    model = "https://i-viaplay-com.akamaized.net/viaplay-prod/609/568/1770635338-c0d013b0b87f8e308a711cb85b996933b80b6c45.jpg?width=400&height=600",
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 110.dp, height = 165.dp)
                        .offset(x = (-80).dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Right poster (middle)
                AsyncImage(
                    model = "https://i-viaplay-com.akamaized.net/viaplay-prod/867/288/1769507332-592292aa4007214b107153e49315d1f2ccfcbf58.jpg?width=400&height=600",
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 110.dp, height = 165.dp)
                        .offset(x = 80.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )

                // Middle poster (highest) - Paradise Hotel
                AsyncImage(
                    model = "https://i-viaplay-com.akamaized.net/viaplay-prod/468/948/1765544051-b83d0f2dcad01079f7822621644f30cd6ab13bdf.jpg?width=400&height=600",
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 140.dp, height = 210.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun CategoryButton(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HorizontalContentSection(title: String, items: List<ContentItem>) {
    Column {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { item ->
                CategoryCardMock(
                    title = item.title,
                    subtitle = item.subtitle,
                    imageUrl = item.imageUrl
                )
            }
        }
    }
}

@Composable
fun CategoryCardMock(title: String, subtitle: String?, imageUrl: String?) {
    Column(modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f/3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (subtitle != null) {
            Text(subtitle, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(title, color = Color.White, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
    }
}
