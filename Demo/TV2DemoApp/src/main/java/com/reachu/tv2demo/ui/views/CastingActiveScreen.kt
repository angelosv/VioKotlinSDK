package com.reachu.tv2demo.ui.views

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reachu.tv2demo.casting.CastingManager
import com.reachu.tv2demo.services.WebSocketManager
import com.reachu.tv2demo.services.events.ContestEventData
import com.reachu.tv2demo.services.events.PollEventData
import com.reachu.tv2demo.services.events.ProductEventData
import com.reachu.tv2demo.services.chat.ChatManager
import com.reachu.tv2demo.ui.components.CastingChatPanel
import com.reachu.tv2demo.ui.model.Match
import com.reachu.tv2demo.ui.theme.TV2Theme
import io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.toDomainProduct
import io.reachu.sdk.core.SdkClient
import io.reachu.VioEngagementUI.Components.VioEngagementContestCard
import io.reachu.VioEngagementUI.Components.VioEngagementPollCard
import io.reachu.VioEngagementUI.Components.VioEngagementProductCard
import io.reachu.VioEngagementSystem.models.Contest as EngagementContest
import io.reachu.VioEngagementSystem.models.ContestType
import io.reachu.VioEngagementSystem.models.Poll as EngagementPoll
import io.reachu.VioEngagementSystem.models.PollOption as EngagementPollOption
import io.reachu.sdk.domain.models.ProductDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CastingActiveView(
    match: Match,
    cartManager: CartManager,
    sdkClient: SdkClient,
    onClose: () -> Unit,
) {
    val castingManager = remember { CastingManager.shared }
    val webSocketManager = remember { WebSocketManager() }
    val poll by webSocketManager.currentPoll.collectAsState()
    val product by webSocketManager.currentProduct.collectAsState()
    val contest by webSocketManager.currentContest.collectAsState()

    DisposableEffect(Unit) {
        webSocketManager.connect()
        onDispose { webSocketManager.disconnect() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0017), Color(0xFF1C0B2E)),
                ),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderSection(match = match, onClose = onClose)
        MatchInfo(match = match)
        Spacer(modifier = Modifier.height(8.dp))
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
        CastingChatPanel(chatManager = remember { ChatManager() }, modifier = Modifier.fillMaxWidth())
    }

    VioFloatingCartIndicator(
        cartManager = cartManager,
        modifier = Modifier.fillMaxSize(),
        customPadding = androidx.compose.foundation.layout.PaddingValues(end = 16.dp, bottom = 100.dp),
    )
}

@Composable
private fun HeaderSection(match: Match, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(match.title, style = TV2Theme.Typography.title, color = Color.White)
            Text(match.subtitle, style = TV2Theme.Typography.small, color = Color.White.copy(alpha = 0.7f))
        }
        IconButton(onClick = onClose) {
            Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun MatchInfo(match: Match) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(TV2Theme.CornerRadius.medium))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TeamBadge(match.homeTeam.name, match.homeTeam.logo)
            Text("VS", style = TV2Theme.Typography.title, color = Color.White)
            TeamBadge(match.awayTeam.name, match.awayTeam.logo)
        }
        Text(match.venue, style = TV2Theme.Typography.caption, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun TeamBadge(name: String, logo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = logo, contentDescription = name, modifier = Modifier.size(64.dp))
        Text(name, style = TV2Theme.Typography.small, color = Color.White, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun InteractionCards(
    poll: PollEventData?,
    product: ProductEventData?,
    contest: ContestEventData?,
    sdkClient: SdkClient,
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
                onVote = { /* La demo solo muestra la UI; el envío real del voto se puede conectar más adelante. */ },
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
                onJoin = { /* Integración real con EngagementManager pendiente en la demo. */ },
                onDismiss = onDismissContest,
            )
        }
        else -> PlaceholderCard()
    }
}

@Composable
private fun PlaceholderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(TV2Theme.CornerRadius.medium))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Ingen aktiviteter akkurat nå", style = TV2Theme.Typography.body, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun EngagementProductFromEvent(
    productEvent: ProductEventData,
    sdkClient: SdkClient,
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
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(TV2Theme.CornerRadius.medium))
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
    sdk: SdkClient,
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
