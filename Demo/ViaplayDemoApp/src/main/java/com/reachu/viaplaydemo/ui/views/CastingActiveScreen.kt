package com.reachu.viaplaydemo.ui.views

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reachu.viaplaydemo.casting.CastingManager
import com.reachu.viaplaydemo.services.WebSocketManager
import com.reachu.viaplaydemo.services.events.ContestEventData
import com.reachu.viaplaydemo.services.events.PollEventData
import com.reachu.viaplaydemo.services.events.ProductEventData
import com.reachu.viaplaydemo.services.chat.ChatManager
import com.reachu.viaplaydemo.ui.model.Match
import com.reachu.viaplaydemo.ui.model.CastingDemoData
import com.reachu.viaplaydemo.ui.model.CastingEventType
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme
import io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.toDomainProduct
import io.reachu.sdk.core.VioSdkClient
import io.reachu.VioEngagementUI.Components.VioEngagementContestCard
import io.reachu.VioEngagementUI.Components.VioEngagementPollCard
import io.reachu.VioEngagementUI.Components.VioEngagementProductCard
import io.reachu.VioEngagementSystem.models.Contest as EngagementContest
import io.reachu.VioEngagementSystem.models.ContestType
import io.reachu.VioEngagementSystem.models.Poll as EngagementPoll
import io.reachu.VioEngagementSystem.models.PollOption as EngagementPollOption
import io.reachu.sdk.domain.models.ProductDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults

val BrandPink = Color(0xFFF5146B)
val BrandDarkBg = Color(0xFF1B1B25)
val BrandDarkerBg = Color(0xFF12121A)

@Composable
fun CastingActiveView(
    match: Match,
    cartManager: CartManager,
    sdkClient: VioSdkClient,
    onClose: () -> Unit,
) {
    val castingManager = remember { CastingManager.shared }
    val webSocketManager = remember { WebSocketManager() }
    val chatManager = remember { ChatManager() }
    
    val poll by webSocketManager.currentPoll.collectAsState()
    val product by webSocketManager.currentProduct.collectAsState()
    val contest by webSocketManager.currentContest.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Chat", "Highlights", "Statistics", "Interaktivt", "Live Scores")
    val tabIcons = listOf(
        Icons.Default.GridView,
        Icons.Default.ChatBubbleOutline,
        Icons.Default.PlayArrow,
        Icons.Default.BarChart,
        Icons.Default.PanTool,
        Icons.Default.EmojiEvents
    )

    DisposableEffect(Unit) {
        webSocketManager.connect()
        onDispose { webSocketManager.disconnect() }
    }

    Box(modifier = Modifier.fillMaxSize().background(BrandDarkBg)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            HeaderSection(match = match, onClose = onClose)
            
            // Tabs Section
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BrandPink,
                        height = 3.dp
                    )
                },
                divider = {
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedTab == index) BrandPink else Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Expanded Content Area
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (selectedTab) {
                    0 -> KampenStarterSnartCard()
                    1 -> CastingChatView(CastingDemoData.mockChatMessages)
                    2 -> CastingLineupView(CastingDemoData.mockLineup)
                    3 -> CastingStatisticsView(CastingDemoData.mockStatistics)
                    4 -> CastingInteraktivtView {
                        InteractionCards(
                            poll = poll,
                            product = product,
                            contest = contest,
                            sdkClient = sdkClient,
                            cartManager = cartManager,
                            onDismissPoll = { webSocketManager.dismissPoll() },
                            onDismissProduct = { webSocketManager.dismissProduct() },
                            onDismissContest = { webSocketManager.dismissContest() },
                        )
                    }
                    5 -> CastingLiveResultsView(CastingDemoData.mockLiveMatches)
                }
            }

            // Bottom Player & Chat Input Controls
            CastingBottomControls(chatManager = chatManager)
        }

        VioFloatingCartIndicator(
            cartManager = cartManager,
            modifier = Modifier.fillMaxSize(),
            customPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
        )
        
        // Mock Floating Likes layer
        FloatingLikesLayer()
    }
}

@Composable
private fun HeaderSection(match: Match, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button & Sponsor
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.align(Alignment.CenterStart).clickable { onClose() }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tilbake", color = Color.White, fontSize = 14.sp)
            }
            
            // "Sponset av ELKJOP"
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                Text("Sponset av", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                // In a real app we'd load the Elkjøp logo. Using text placeholder.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ELKJØP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(14.dp)) // Chevron mock
                }
            }
            
            // Close Button
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd).background(Color.White.copy(alpha = 0.1f), CircleShape).size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Match Teams & Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Team
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                // Team Logo
                Box(
                    modifier = Modifier.size(60.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = com.reachu.viaplaydemo.utils.LogoResolver.resolveLogo("barcelona_logo"),
                        contentDescription = "Barcelona",
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("FC Barcelona", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            // Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(6.dp).background(BrandPink, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("0", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("-15'", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }

            // Away Team
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(60.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = com.reachu.viaplaydemo.utils.LogoResolver.resolveLogo("psg_logo"),
                        contentDescription = "PSG",
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Paris Saint-Germain", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Match Metadata
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Champions League", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Camp Nou", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun KampenStarterSnartCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFF251A22), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BrandPink.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = BrandPink, modifier = Modifier.size(20.dp))
                }
                Text("Kampen starter snart", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            
            Text(
                "Startoppstillingene kommer snart. Følg med her for å se alle hendelsene, kommentarer og høydepunkter fra kampen i sanntid.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun CastingBottomControls(chatManager: ChatManager) {
    var chatMessage by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandDarkerBg)
            .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chat Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier.size(36.dp).background(BrandPink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                TextField(
                    value = chatMessage,
                    onValueChange = { chatMessage = it },
                    placeholder = { Text("Send en melding...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                )
            }

            // Like Button
            IconButton(
                onClick = {
                    floatingLikesList.add(FloatingLike(xOffset = (-40..40).random().toFloat()))
                },
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).size(44.dp)
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = BrandPink, modifier = Modifier.size(20.dp))
            }
            
            // Send / Options
            IconButton(
                onClick = { /* Send message action */ },
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).size(44.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        // Video Timeline
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            ) {
                // Circle at start to indicate tracking, usually syncs with video progress
                Box(modifier = Modifier.size(12.dp).background(Color.White, CircleShape).align(Alignment.CenterStart).offset(x = (-4).dp))
            }

            // Time Indicators
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("-15'", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("90'", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LIVE Button Left
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // Center Controls
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause", 
                    tint = Color.White, 
                    modifier = Modifier.size(36.dp).clickable { isPlaying = !isPlaying }
                )
                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Volume Control
            Icon(Icons.Default.VolumeOff, contentDescription = "Mute", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}


data class FloatingLike(val id: String = UUID.randomUUID().toString(), val xOffset: Float)

// Global state for simplicity in demo
val floatingLikesList = mutableStateListOf<FloatingLike>()

@Composable
fun FloatingLikesLayer() {
    // A simple presentation layer that draws flying thumbs up.
    Box(modifier = Modifier.fillMaxSize()) {
        floatingLikesList.forEach { like ->
            var yOffset by remember { mutableStateOf(0f) }
            var opacity by remember { mutableStateOf(1f) }
            
            LaunchedEffect(like.id) {
                yOffset = -300f
                opacity = 0f
                delay(2000)
                floatingLikesList.remove(like)
            }
            
            Icon(
                Icons.Default.ThumbUp,
                contentDescription = null,
                tint = BrandPink.copy(alpha = opacity),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 80.dp, bottom = 140.dp)
                    .offset(x = like.xOffset.dp, y = yOffset.dp)
            )
        }
    }
}

@Composable
private fun InteractionCards(
    poll: PollEventData?,
    product: ProductEventData?,
    contest: ContestEventData?,
    sdkClient: VioSdkClient,
    cartManager: CartManager,
    onDismissPoll: () -> Unit,
    onDismissProduct: () -> Unit,
    onDismissContest: () -> Unit,
) {
    when {
        poll != null -> {
            val engagementPoll = remember(poll.id) {
                EngagementPoll(
                    id = poll.id,
                    broadcastId = "demo-broadcast",
                    question = poll.question,
                    options = poll.options.mapIndexed { index, option ->
                        EngagementPollOption(
                            id = index.toString(),
                            text = option.text,
                        )
                    },
                )
            }

            VioEngagementPollCard(
                poll = engagementPoll,
                pollResults = null,
                sponsorLogoUrl = poll.campaignLogo,
                onVote = { },
                onDismiss = onDismissPoll,
            )
        }
        product != null -> EngagementProductFromEvent(
            productEvent = product,
            sdkClient = sdkClient,
            currency = cartManager.currency,
            country = cartManager.country,
            onAddToCart = { productDto ->
                productDto?.let { cartManager.addProductAsync(it.toDomainProduct(), 1, null) }
            },
            onDismiss = onDismissProduct,
        )
        contest != null -> {
            val engagementContest = remember(contest.id) {
                EngagementContest(
                    id = contest.id,
                    broadcastId = "demo-broadcast",
                    title = contest.name,
                    description = "",
                    prize = contest.prize,
                    contestType = ContestType.giveaway,
                    endTime = contest.deadline,
                )
            }

            VioEngagementContestCard(
                contest = engagementContest,
                sponsorLogoUrl = contest.campaignLogo,
                onJoin = { },
                onDismiss = onDismissContest,
            )
        }
    }
}

@Composable
private fun EngagementProductFromEvent(
    productEvent: ProductEventData,
    sdkClient: VioSdkClient,
    currency: String,
    country: String,
    onAddToCart: (ProductDto?) -> Unit,
    onDismiss: () -> Unit,
) {
    var product by remember(productEvent.id) { mutableStateOf<ProductDto?>(null) }
    var isLoading by remember(productEvent.id) { mutableStateOf(true) }

    LaunchedEffect(productEvent.id) {
        isLoading = true
        product = fetchProductForEngagement(productEvent, sdkClient, currency, country)
        isLoading = false
        if (product == null) {
            onDismiss()
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(ViaplayTheme.CornerRadius.medium))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }
    } else {
        val currentProduct = product
        if (currentProduct != null) {
            VioEngagementProductCard(
                product = currentProduct,
                sponsorLogoUrl = productEvent.campaignLogo,
                onAddToCart = { onAddToCart(currentProduct) },
                onDismiss = onDismiss,
            )
        }
    }
}

private suspend fun fetchProductForEngagement(
    event: ProductEventData,
    sdk: VioSdkClient,
    currency: String,
    country: String,
): ProductDto? = withContext(Dispatchers.IO) {
    val id = event.productId.toIntOrNull() ?: return@withContext null
    runCatching {
        sdk.channel.product.getByIds(
            productIds = listOf(id),
            currency = currency,
            imageSize = "large",
            useCache = false,
            shippingCountryCode = country,
        )
    }.getOrNull()?.firstOrNull()
}
