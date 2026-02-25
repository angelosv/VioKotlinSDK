package io.reachu.VioUI

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.reachu.VioUI.Components.CheckoutDraft
import io.reachu.VioUI.Components.CheckoutPrefill
import io.reachu.VioUI.Components.VioCheckoutOverlayController
import io.reachu.VioUI.Components.VioCheckoutOverlayController.CheckoutStep
import io.reachu.VioUI.Components.VioCheckoutOverlayController.PaymentMethod
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.CartItem
import io.reachu.VioUI.Managers.ToastManager
import io.reachu.VioUI.Managers.setShippingOption
import io.reachu.VioUI.Managers.clearCart
import io.reachu.VioUI.Managers.resetCartAndCreateNew
import io.reachu.VioUI.Managers.discountApplyOrCreate
import io.reachu.VioUI.Managers.vippsInit
import io.reachu.VioUI.Managers.applyCheapestShippingPerSupplier
import io.reachu.VioUI.Managers.refreshShippingOptions
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
// Stripe PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import io.reachu.VioUI.Components.compose.buttons.VioButton
import io.reachu.VioUI.Components.compose.theme.adaptiveVioColors
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Components.VioButtonModel
import io.reachu.VioUI.PaymentSheetBridge
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.KlarnaMode
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioUI.Managers.VippsPaymentHandler

private fun io.reachu.VioCore.configuration.TypographyToken.toComposeTextStyle(): androidx.compose.ui.text.TextStyle {
    val weight = when (fontWeight.lowercase()) {
        "bold" -> androidx.compose.ui.text.font.FontWeight.Bold
        "semibold" -> androidx.compose.ui.text.font.FontWeight.SemiBold
        "medium" -> androidx.compose.ui.text.font.FontWeight.Medium
        else -> androidx.compose.ui.text.font.FontWeight.Normal
    }
    return androidx.compose.ui.text.TextStyle(
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
    )
}

@Composable
fun VippsProcessingBanner(
    status: VippsPaymentHandler.PaymentStatus,
    modifier: Modifier = Modifier,
) {
    val message = when (status) {
        VippsPaymentHandler.PaymentStatus.SUCCESS -> "Vipps payment confirmed"
        VippsPaymentHandler.PaymentStatus.FAILED -> "Vipps payment failed"
        VippsPaymentHandler.PaymentStatus.CANCELLED -> "Vipps payment canceled"
        else -> "Processing Vipps payment…"
    }
    val detail = when (status) {
        VippsPaymentHandler.PaymentStatus.UNKNOWN,
        VippsPaymentHandler.PaymentStatus.IN_PROGRESS -> "Stay on this screen while we verify the payment."
        VippsPaymentHandler.PaymentStatus.SUCCESS -> "You're all set! Finalizing checkout…"
        VippsPaymentHandler.PaymentStatus.CANCELLED -> "No charges were made."
        VippsPaymentHandler.PaymentStatus.FAILED -> "You can close this screen and try another method."
    }

    // Full-screen overlay background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = false) { }, // Block clicks
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    color = VioColors.primary.toColor(),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        message,
                        style = VioTypography.title3.toComposeTextStyle(),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        detail,
                        style = VioTypography.body.toComposeTextStyle(),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun String.toColor(): androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VioCheckoutOverlay(
    cartManager: CartManager,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    initialStep: CheckoutStep = VioCheckoutOverlayController.CheckoutStep.OrderSummary,
    checkoutDraft: CheckoutDraft? = null,
    prefill: CheckoutPrefill? = null,
    isCampaignGated: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val dsColors = adaptiveVioColors()
    val defaultDraft = remember { CheckoutDraft() }
    val draft = checkoutDraft ?: defaultDraft
    val overlay = remember(cartManager, draft, prefill) {
        VioCheckoutOverlayController(
            cartManager,
            draft,
            scope,
            prefill
        )
    }
    val campaignManager = remember { CampaignManager.shared }
    val campaignActive by campaignManager.isCampaignActive.collectAsState(initial = true)
    val currentCampaign by campaignManager.currentCampaign.collectAsState(initial = null)
    val vippsInProgress by VippsPaymentHandler.isPaymentInProgress.collectAsState(initial = false)
    val vippsStatus by VippsPaymentHandler.paymentStatus.collectAsState(initial = VippsPaymentHandler.PaymentStatus.UNKNOWN)

    val shouldShow = if (isCampaignGated) {
        VioConfiguration.shared.shouldUseSDK && campaignActive && currentCampaign?.isPaused != true
    } else {
        true
    }
    
    if (!shouldShow) {
        return
    }

    // Step state is now managed directly by the overlay controller
    LaunchedEffect(Unit) {
        overlay.goToStep(initialStep)
    }

    var paymentMethod by remember { mutableStateOf(overlay.selectedPaymentMethod) }
    val allowedMethodsSnapshot = overlay.allowedPaymentMethods
    LaunchedEffect(allowedMethodsSnapshot) {
        if (allowedMethodsSnapshot.isNotEmpty() && !allowedMethodsSnapshot.contains(paymentMethod)) {
            val fallback = allowedMethodsSnapshot.first()
            paymentMethod = fallback
            overlay.selectPaymentMethod(fallback)
        }
    }

    // Handle deep links from web providers (Klarna/Vipps)
    LaunchedEffect(Unit) {
        CheckoutDeepLinkBus.events.collect { ev ->
            when (ev.status) {
                CheckoutDeepLinkBus.Status.Success -> {
                    overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Processing)
                    overlay.updateCheckout(
                        paymentMethod = paymentMethod,
                        advanceToSuccess = true,
                        status = "paid",
                    ) { res ->
                        res.onSuccess {
                            VippsPaymentHandler.stopPaymentTracking()
                            scope.launch { cartManager.clearCart() }
                            overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Success)
                        }
                        res.onFailure {
                            VippsPaymentHandler.stopPaymentTracking()
                            ToastManager.showError("Checkout update failed")
                        }
                    }
                }

                CheckoutDeepLinkBus.Status.Cancel -> {
                    VippsPaymentHandler.stopPaymentTracking()
                    ToastManager.showInfo("Payment canceled")
                    overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Error)
                }
                else -> {}
            }
        }
    }

    // Address fields (bound to draft on continue)
    var firstName by remember { mutableStateOf(draft.firstName) }
    var lastName by remember { mutableStateOf(draft.lastName) }
    var email by remember { mutableStateOf(draft.email) }
    var phone by remember { mutableStateOf(draft.phone) }
    // Keep only digits in the dialing code to avoid "++47"
    var phoneCode by remember { mutableStateOf(cartManager.phoneCode.filter { it.isDigit() }) }
    var address1 by remember { mutableStateOf(draft.address1) }
    var address2 by remember { mutableStateOf(draft.address2) }
    var city by remember { mutableStateOf(draft.city) }
    var province by remember { mutableStateOf(draft.province) }
    var country by remember {
        mutableStateOf(
            cartManager.selectedMarket?.name ?: draft.countryName
        )
    }
    var zip by remember { mutableStateOf(draft.zip) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var editingAddress by remember { mutableStateOf(false) }

    // Apply pretty hardcoded defaults for NO and US when draft is empty
    LaunchedEffect(cartManager.country, cartManager.currency) {
        fun applyDefaults(
            fn: String,
            ln: String,
            em: String,
            phCode: String,
            ph: String,
            addr1: String,
            addr2: String,
            c: String,
            st: String,
            z: String,
            countryNameDefault: String
        ) {
            if (draft.firstName.isBlank() && draft.lastName.isBlank() && draft.address1.isBlank()) {
                firstName = fn
                lastName = ln
                email = em
                phoneCode = phCode
                phone = ph
                address1 = addr1
                address2 = addr2
                city = c
                province = st
                zip = z
                country = countryNameDefault
                overlay.updateAddressDraft(
                    firstName = fn, lastName = ln, email = em, phone = ph,
                    address1 = addr1, address2 = addr2, city = c, province = st,
                    zip = z
                )
            }
        }

        val isNorway =
            cartManager.country.equals("NO", true) || cartManager.currency.equals("NOK", true)
        when {
            isNorway -> applyDefaults(
                fn = "John", ln = "Doe", em = "john.doe@example.com",
                phCode = "47", ph = "2125551212",
                addr1 = "Karl Johans gate 15", addr2 = "",
                c = "Oslo", st = "Oslo", z = "0154",
                countryNameDefault = "Norway",
            )

            cartManager.country.equals("US", true) || cartManager.currency.equals(
                "USD",
                true
            ) -> applyDefaults(
                fn = "John", ln = "Doe", em = "john.doe@example.com",
                phCode = "1", ph = "2125551212",
                addr1 = "82 Melora Street", addr2 = "",
                c = "Westbridge", st = "California", z = "92841",
                countryNameDefault = "United States",
            )
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        )
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
            // Tap outside to dismiss
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() }
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    // Handle bar (tap to dismiss)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Box(
                            Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.LightGray)
                                .clickable { onBack() }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Centered title with back arrow at extreme left
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            "Checkout",
                            style = VioTypography.title2.toComposeTextStyle(),
                            color = dsColors.textPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Observe cart changes for reactive UI (items, totals)
                    var items by remember { mutableStateOf(cartManager.items) }
                    var shippingTotal by remember { mutableStateOf(cartManager.shippingTotal) }
                    var cartTotal by remember { mutableStateOf(cartManager.cartTotal) }
                    LaunchedEffect(cartManager) {
                        snapshotFlow { cartManager.items }.collectLatest { items = it }
                    }
                    LaunchedEffect(cartManager) {
                        snapshotFlow { cartManager.shippingTotal }.collectLatest {
                            shippingTotal = it
                        }
                    }
                    LaunchedEffect(cartManager) {
                        snapshotFlow { cartManager.cartTotal }.collectLatest { cartTotal = it }
                    }
                    LaunchedEffect(items.size) {
                        if (items.isNotEmpty()) {
                            runCatching { cartManager.refreshShippingOptions() }
                        }
                    }

                    // Local UI mirrors deprecate polling y siguen al manager reactivamente
                    var itemsState by remember { mutableStateOf(cartManager.items) }
                    var shippingTotalState by remember { mutableStateOf(cartManager.shippingTotal) }
                    var cartTotalState by remember { mutableStateOf(cartManager.cartTotal) }
                    var subtotalState by remember { mutableStateOf(cartManager.subtotal) }
                    var taxTotalState by remember { mutableStateOf(cartManager.taxTotal) }
                    var discountTotalState by remember { mutableStateOf(cartManager.discountTotal) }
                    
                    LaunchedEffect(cartManager) {
                        snapshotFlow {
                            listOf(
                                cartManager.items,
                                cartManager.shippingTotal,
                                cartManager.cartTotal,
                                cartManager.subtotal,
                                cartManager.taxTotal,
                                cartManager.discountTotal
                            )
                        }
                            .collect { _ ->
                                itemsState = cartManager.items
                                shippingTotalState = cartManager.shippingTotal
                                cartTotalState = cartManager.cartTotal
                                subtotalState = cartManager.subtotal
                                taxTotalState = cartManager.taxTotal
                                discountTotalState = cartManager.discountTotal
                            }
                    }

                    // Subtle action locking + tiny loaders while a cart mutation is in progress
                    var busyIds by remember { mutableStateOf(setOf<String>()) }
                    var managerLoading by remember { mutableStateOf(cartManager.isLoading) }
                    LaunchedEffect(cartManager) {
                        snapshotFlow { cartManager.isLoading }.collect { loading ->
                            managerLoading = loading
                            if (!loading && busyIds.isNotEmpty()) busyIds = emptySet()
                        }
                    }

                    // Helper to clear busy state when the expected cart change is observed (qty or removal)
                    fun trackBusyUntilApplied(
                        itemId: String,
                        expectedQty: Int?,
                        removal: Boolean = false
                    ) {
                        scope.launch {
                            snapshotFlow { cartManager.items }.collect { currentList ->
                                val current = currentList.firstOrNull { it.id == itemId }
                                val fulfilled = when {
                                    removal -> current == null
                                    expectedQty != null -> current?.quantity == expectedQty
                                    else -> !cartManager.isLoading
                                }
                                if (fulfilled) {
                                    busyIds = busyIds - itemId
                                    cancel()
                                }
                            }
                        }
                    }

                    fun ensureSingleShippingSelection(source: List<CartItem>) {
                        var changed = false
                        source.forEach { item ->
                            if (item.shippingId.isNullOrBlank() && item.availableShippings.size == 1) {
                                val option = item.availableShippings.first()
                                cartManager.setShippingOption(item.id, option.id)
                                changed = true
                            }
                        }
                        if (changed) {
                            itemsState = cartManager.items
                            shippingTotalState = cartManager.shippingTotal
                            cartTotalState = cartManager.cartTotal
                        }
                    }
                    LaunchedEffect(items) {
                        if (items.isNotEmpty()) {
                            ensureSingleShippingSelection(items)
                        }
                    }

                    when (overlay.currentStep) {
                        VioCheckoutOverlayController.CheckoutStep.Processing -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        VioCheckoutOverlayController.CheckoutStep.OrderSummary -> {
                            Box(Modifier.fillMaxSize()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = 84.dp) // leave room for fixed CTA
                                ) {
                                    Text("Cart", fontWeight = FontWeight.SemiBold)
                                    itemsState.forEach { item ->
                                        Card {
                                            Column(Modifier.padding(12.dp)) {
                                                // Top row: image + title/brand at left, price at right
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        Modifier.weight(1f),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            10.dp
                                                        )
                                                    ) {
                                                        if (!item.imageUrl.isNullOrBlank()) {
                                                            AsyncImage(
                                                                model = item.imageUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.height(48.dp)
                                                            )
                                                        }
                                                        Column(Modifier.weight(1f)) {
                                                            item.brand?.let {
                                                                Text(
                                                                    it,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Text(
                                                                item.title,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 2
                                                            )
                                                            val vTitle = item.variantTitle
                                                            if (!vTitle.isNullOrBlank()) {
                                                                Text(
                                                                    vTitle,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        "${cartManager.currency} ${
                                                            String.format(
                                                                "%.2f",
                                                                item.price
                                                            )
                                                        }", fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                Spacer(Modifier.height(8.dp))

                                                // Quantity stepper + optional delete
                                                val isBusy = managerLoading || (item.id in busyIds)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (item.quantity > 1) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                busyIds = busyIds + item.id
                                                                val nextQty =
                                                                    (item.quantity - 1).coerceAtLeast(
                                                                        0
                                                                    )
                                                                cartManager.updateQuantityAsync(
                                                                    item,
                                                                    nextQty
                                                                )
                                                                trackBusyUntilApplied(
                                                                    item.id,
                                                                    nextQty
                                                                )
                                                            },
                                                            enabled = !isBusy,
                                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                                12.dp
                                                            )
                                                        ) {
                                                            if (item.id in busyIds) {
                                                                CircularProgressIndicator(
                                                                    strokeWidth = 2.dp,
                                                                    modifier = Modifier.height(16.dp)
                                                                        .width(16.dp)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    Icons.Filled.Remove,
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = {
                                                                busyIds = busyIds + item.id
                                                                cartManager.removeItemAsync(item)
                                                                trackBusyUntilApplied(
                                                                    item.id,
                                                                    expectedQty = null,
                                                                    removal = true
                                                                )
                                                            },
                                                            enabled = !isBusy,
                                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                                12.dp
                                                            )
                                                        ) {
                                                            if (item.id in busyIds) {
                                                                CircularProgressIndicator(
                                                                    strokeWidth = 2.dp,
                                                                    modifier = Modifier.height(16.dp)
                                                                        .width(16.dp)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    Icons.Outlined.Delete,
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        item.quantity.toString(),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    OutlinedButton(
                                                        onClick = {
                                                            busyIds = busyIds + item.id
                                                            val nextQty = item.quantity + 1
                                                            cartManager.updateQuantityAsync(
                                                                item,
                                                                nextQty
                                                            )
                                                            trackBusyUntilApplied(item.id, nextQty)
                                                        },
                                                        enabled = !isBusy,
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                            12.dp
                                                        )
                                                    ) {
                                                        if (item.id in busyIds) {
                                                            CircularProgressIndicator(
                                                                strokeWidth = 2.dp,
                                                                modifier = Modifier.height(16.dp)
                                                                    .width(16.dp)
                                                            )
                                                        } else {
                                                            Icon(
                                                                Icons.Filled.Add,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.weight(1f))
                                                    // Keep explicit delete at right only when quantity > 1
                                                    if (item.quantity > 1) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                busyIds = busyIds + item.id
                                                                cartManager.removeItemAsync(item)
                                                                trackBusyUntilApplied(
                                                                    item.id,
                                                                    expectedQty = null,
                                                                    removal = true
                                                                )
                                                            },
                                                            enabled = !isBusy,
                                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                                12.dp
                                                            )
                                                        ) {
                                                            if (item.id in busyIds) {
                                                                CircularProgressIndicator(
                                                                    strokeWidth = 2.dp,
                                                                    modifier = Modifier.height(16.dp)
                                                                        .width(16.dp)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    Icons.Outlined.Delete,
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(Modifier.height(6.dp))
                                                val totalForItem = item.price * item.quantity
                                                Text(
                                                    "Total for this item: ${cartManager.currency} ${
                                                        String.format(
                                                            "%.2f",
                                                            totalForItem
                                                        )
                                                    }",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider()
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Shipping Address", fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = {
                                            if (editingAddress) {
                                                // Save current edits into draft and close
                                                overlay.updateAddressDraft(
                                                    firstName = firstName,
                                                    lastName = lastName,
                                                    address1 = address1,
                                                    address2 = address2,
                                                    city = city,
                                                    province = province,
                                                    zip = zip,
                                                    phone = phone,
                                                    email = email,
                                                    company = null,
                                                )
                                                overlay.toggleEditAddress()
                                                editingAddress = false
                                            } else {
                                                overlay.toggleEditAddress()
                                                editingAddress = true
                                            }
                                        }) {
                                            Icon(
                                                if (editingAddress) Icons.Filled.Check else Icons.Filled.Edit,
                                                null
                                            )
                                        }
                                    }
                                    if (!editingAddress) {
                                        val dispFirst = draft.firstName.ifBlank { firstName }
                                        val dispLast = draft.lastName.ifBlank { lastName }
                                        val dispAddr1 = draft.address1.ifBlank { address1 }
                                        val dispAddr2 = draft.address2.ifBlank { address2 }
                                        val dispCity = draft.city.ifBlank { city }
                                        val dispProv = draft.province.ifBlank { province }
                                        val dispZip = draft.zip.ifBlank { zip }
                                        val dispCountry = draft.countryName.ifBlank { country }
                                        val dispPhone = draft.phone.ifBlank { phone }
                                        val isUSDisp = cartManager.country.equals(
                                            "US",
                                            true
                                        ) || cartManager.currency.equals(
                                            "USD",
                                            true
                                        ) || dispCountry.contains("United States", true)
                                        val locality = buildString {
                                            append(dispCity)
                                            if (isUSDisp && dispProv.isNotBlank()) append(", ").append(
                                                dispProv
                                            )
                                            append(", ").append(dispCountry)
                                        }
                                        Column {
                                            Text("$dispFirst $dispLast")
                                            Text(dispAddr1)
                                            if (dispAddr2.isNotBlank()) Text(dispAddr2)
                                            Text(locality)
                                            Text(dispZip)
                                            Text("Phone : +${phoneCode.filter { it.isDigit() }} $dispPhone")
                                        }
                                    } else {
                                        // Inline edit form (same fields) and save with the check icon
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = firstName,
                                                onValueChange = { firstName = it },
                                                label = { Text("First Name") },
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = lastName,
                                                onValueChange = { lastName = it },
                                                label = { Text("Last Name") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = email,
                                            onValueChange = { email = it },
                                            label = { Text("Email") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val flag = cartManager.flagURL
                                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                                Row(
                                                    Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (!flag.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = flag,
                                                            contentDescription = null,
                                                            modifier = Modifier.height(18.dp)
                                                        )
                                                        Spacer(Modifier.padding(horizontal = 4.dp))
                                                    }
                                                    Text("${cartManager.phoneCode}")
                                                }
                                            }
                                            OutlinedTextField(
                                                value = phone,
                                                onValueChange = { phone = it },
                                                label = { Text("Phone") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = address1,
                                            onValueChange = { address1 = it },
                                            label = { Text("Address") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = address2,
                                            onValueChange = { address2 = it },
                                            label = { Text("Apt, suite, etc. (optional)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        val isUSEdit = cartManager.country.equals(
                                            "US",
                                            true
                                        ) || cartManager.currency.equals(
                                            "USD",
                                            true
                                        ) || country.contains("United States", true)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = city,
                                                onValueChange = { city = it },
                                                label = { Text("City") },
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isUSEdit) {
                                                OutlinedTextField(
                                                    value = province,
                                                    onValueChange = { province = it },
                                                    label = { Text("State") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = zip,
                                                    onValueChange = { zip = it },
                                                    label = { Text("ZIP") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                OutlinedTextField(
                                                    value = zip,
                                                    onValueChange = { zip = it },
                                                    label = { Text("Postal Code") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = country,
                                            onValueChange = { country = it },
                                            label = { Text("Country") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Fields will be saved when tapping the check icon above
                                    }

                                    HorizontalDivider()
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Shipping Options",
                                            style = VioTypography.title3.toComposeTextStyle(),
                                            color = dsColors.textPrimary
                                        )
                                        // Required pill
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(
                                                    0xFFE6F0FF
                                                )
                                            )
                                        ) {
                                            Text(
                                                "Required",
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    itemsState.forEach { item ->
                                        Column(
                                            Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                item.title.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val vTitle = item.variantTitle
                                            if (!vTitle.isNullOrBlank()) {
                                                Text(
                                                    vTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            item.availableShippings?.forEach { opt ->
                                                val selected = item.shippingId == opt.id
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFFF1F3F5))
                                                        .clickable(enabled = !managerLoading) {
                                                            cartManager.setShippingOption(
                                                                item.id,
                                                                opt.id
                                                            )
                                                            itemsState = cartManager.items
                                                            shippingTotalState =
                                                                cartManager.shippingTotal
                                                            cartTotalState = cartManager.cartTotal
                                                        }
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        RadioButton(selected = selected, onClick = {
                                                            cartManager.setShippingOption(
                                                                item.id,
                                                                opt.id
                                                            )
                                                            itemsState = cartManager.items
                                                            shippingTotalState =
                                                                cartManager.shippingTotal
                                                            cartTotalState = cartManager.cartTotal
                                                        }, enabled = !managerLoading)
                                                        Spacer(Modifier.width(4.dp))
                                                        Column {
                                                            Text(opt.name ?: "Shipping")
                                                            opt.description?.let {
                                                                Text(
                                                                    it,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                    val priceLabel =
                                                        if ((opt.amount) == 0.0) "Free" else "${opt.currency} ${
                                                            String.format(
                                                                "%.2f",
                                                                opt.amount
                                                            )
                                                        }"
                                                    Text(priceLabel)
                                                }
                                                Spacer(Modifier.height(6.dp))
                                            }
                                        }
                                    }

                                    HorizontalDivider()
                                    Text(
                                        "Shipping",
                                        style = VioTypography.title3.toComposeTextStyle(),
                                        color = dsColors.textPrimary
                                    )
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Total shipping")
                                                val totalShipLabel =
                                                    if (shippingTotalState == 0.0) "Free" else "${cartManager.currencySymbol} ${
                                                        String.format(
                                                            "%.2f",
                                                            shippingTotalState
                                                        )
                                                    }"
                                                Text(totalShipLabel)
                                            }
                                            val firstShip =
                                                itemsState.firstOrNull { it.shippingName != null }
                                            if (firstShip != null) {
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(
                                                            firstShip.shippingName ?: ""
                                                        ); firstShip.shippingDescription?.let {
                                                        Text(
                                                            it,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    }
                                                    val lineAmount = firstShip.shippingAmount ?: 0.0
                                                    val lineLabel =
                                                        if (lineAmount == 0.0) "Free" else "${firstShip.shippingCurrency ?: cartManager.currency} ${
                                                            String.format(
                                                                "%.2f",
                                                                lineAmount
                                                            )
                                                        }"
                                                    Text(lineLabel)
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider()
                                    Text(
                                        "Order Summary",
                                        style = VioTypography.title3.toComposeTextStyle(),
                                        color = dsColors.textPrimary
                                    )
                                     Row(
                                         Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text("Subtotal"); Text(
                                         "${cartManager.currency} ${
                                             String.format(
                                                 "%.2f",
                                                 subtotalState
                                             )
                                         }"
                                     )
                                     }
                                     Row(
                                         Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text("Shipping"); Text(
                                         "${cartManager.currency} ${
                                             String.format(
                                                 "%.2f",
                                                 shippingTotalState
                                             )
                                         }"
                                     )
                                     }
                                     Row(
                                         Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text("Tax"); Text(
                                         "${cartManager.currency} ${
                                             String.format(
                                                 "%.2f",
                                                 taxTotalState
                                             )
                                         }"
                                     )
                                     }
                                     Row(
                                         Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text("Discount")
                                         Text(
                                             "${cartManager.currency} ${
                                                 String.format(
                                                     "%.2f",
                                                     -discountTotalState
                                                 )
                                             }",
                                             color = if (discountTotalState > 0) Color(0xFF2E7D32) else dsColors.textPrimary
                                         )
                                     }
                                     Row(
                                         Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text("Total", fontWeight = FontWeight.SemiBold)
                                         Text(
                                             "${cartManager.currency} ${
                                                 String.format(
                                                     "%.2f",
                                                     cartTotalState
                                                 )
                                             }",
                                             fontWeight = FontWeight.SemiBold,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                     }

                                    // extra space no longer needed (CTA is fixed)
                                }
                                Column(
                                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isEmpty = items.isEmpty()
                                    val itemsMissingShipping =
                                        itemsState.filter { it.shippingId.isNullOrBlank() }
                                    val requiresShippingSelection =
                                        itemsMissingShipping.isNotEmpty()
                                    val canProceed =
                                        !isEmpty && !managerLoading && !requiresShippingSelection
                                    var proceedLoading by remember { mutableStateOf(false) }
                                    if (isEmpty) {
                                        // Info banner like iOS
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(
                                                    0xFFE6F0FF
                                                )
                                            )
                                        ) {
                                            Row(Modifier.padding(12.dp)) {
                                                val infoIcon =
                                                    androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_info)
                                                Icon(
                                                    painter = infoIcon,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2B6CB0)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Your cart is empty. Add products to continue.",
                                                    color = Color(0xFF2B6CB0)
                                                )
                                            }
                                        }
                                    }
                                    if (!isEmpty && requiresShippingSelection) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(
                                                    0xFFFFF4E5
                                                )
                                            )
                                        ) {
                                            Row(Modifier.padding(12.dp)) {
                                                val warnIcon =
                                                    androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_info)
                                                Icon(
                                                    painter = warnIcon,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB26A00)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Select a shipping option for each item before continuing.",
                                                    color = Color(0xFFB26A00)
                                                )
                                            }
                                        }
                                    }
                                    VioButton(
                                        model = VioButtonModel(
                                            title = "Proceed to Checkout",
                                            style = VioButtonModel.Style.Primary,
                                            size = VioButtonModel.Size.Large,
                                            isLoading = proceedLoading,
                                            isDisabled = proceedLoading || !canProceed,
                                            onClick = {
                                                if (!proceedLoading && canProceed) {
                                                    proceedLoading = true
                                                    // Persist latest address fields into draft (mirror Swift before proceeding)
                                                    overlay.updateAddressDraft(
                                                        firstName = firstName,
                                                        lastName = lastName,
                                                        email = email,
                                                        phone = phone,
                                                        address1 = address1,
                                                        address2 = address2,
                                                        city = city,
                                                        province = province,
                                                        zip = zip,
                                                    )
                                                    overlay.updatePhoneCodeFromCart()

                                                    // Apply cheapest shipping, then create and update checkout
                                                    scope.launch {
                                                        runCatching { cartManager.applyCheapestShippingPerSupplier() }
                                                        overlay.proceedToPayment(advanceToReview = false) { res ->
                                                            res.onSuccess {
                                                                overlay.updateCheckout(
                                                                    advanceToSuccess = false
                                                                ) { upd ->
                                                                    upd.onSuccess {
                                                                        overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Review)
                                                                    }
                                                                    upd.onFailure {
                                                                        io.reachu.VioUI.Managers.ToastManager.showError(
                                                                            "Checkout update failed"
                                                                        )
                                                                    }
                                                                    proceedLoading = false
                                                                }
                                                            }
                                                            res.onFailure {
                                                                io.reachu.VioUI.Managers.ToastManager.showError(
                                                                    "Checkout creation failed"
                                                                )
                                                                proceedLoading = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        VioCheckoutOverlayController.CheckoutStep.Review -> {
                            // Review screen matching Swift design (scrollable)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 84.dp)
                            ) {
                                // Cart summary
                                Text("Cart", fontWeight = FontWeight.SemiBold)
                                items.forEach { item ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!item.imageUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = item.imageUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.height(44.dp)
                                                )
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    item.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                val vTitle = item.variantTitle
                                                if (!vTitle.isNullOrBlank()) {
                                                    Text(
                                                        vTitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    "Qty: ${item.quantity}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            "${cartManager.currency} ${
                                                String.format(
                                                    "%.2f",
                                                    item.price * item.quantity
                                                )
                                            }", fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                HorizontalDivider()
                                Text(
                                    "Payment Method",
                                    style = VioTypography.title3.toComposeTextStyle(),
                                    color = dsColors.textPrimary
                                )

                                @Composable
                                fun PaymentCard(
                                    selected: Boolean,
                                    enabled: Boolean,
                                    iconRes: Int,
                                    title: String,
                                    onClick: () -> Unit,
                                ) {
                                    val bg = when {
                                        selected -> Color(0xFFEAF2FF)
                                        enabled -> Color(0xFFF1F3F5)
                                        else -> Color(0xFFF6F6F6)
                                    }
                                    val borderColor =
                                        if (selected) MaterialTheme.colorScheme.primary else Color(
                                            0xFFE3E3E3
                                        )
                                    val shape = RoundedCornerShape(16.dp)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = bg),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape)
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = borderColor,
                                                shape = shape
                                            )
                                            .clickable(enabled = enabled) { onClick() }
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selected,
                                                onClick = { if (enabled) onClick() },
                                                enabled = enabled
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = iconRes),
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ).height(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(title)
                                        }
                                    }
                                }

                                if (overlay.isMethodAllowed(VioCheckoutOverlayController.PaymentMethod.Stripe)) {
                                    PaymentCard(
                                        selected = paymentMethod == VioCheckoutOverlayController.PaymentMethod.Stripe,
                                        enabled = true,
                                        iconRes = R.drawable.ic_stripe,
                                        title = "Credit Card",
                                        onClick = {
                                            paymentMethod =
                                                VioCheckoutOverlayController.PaymentMethod.Stripe; overlay.selectPaymentMethod(
                                            VioCheckoutOverlayController.PaymentMethod.Stripe
                                        )
                                        },
                                    )
                                }
                                if (overlay.isMethodAllowed(VioCheckoutOverlayController.PaymentMethod.Klarna)) {
                                    PaymentCard(
                                        selected = paymentMethod == VioCheckoutOverlayController.PaymentMethod.Klarna,
                                        enabled = true,
                                        iconRes = R.drawable.ic_klarna,
                                        title = "Pay with Klarna",
                                        onClick = {
                                            paymentMethod =
                                                VioCheckoutOverlayController.PaymentMethod.Klarna; overlay.selectPaymentMethod(
                                            VioCheckoutOverlayController.PaymentMethod.Klarna
                                        )
                                        },
                                    )
                                }
                                if (overlay.isMethodAllowed(VioCheckoutOverlayController.PaymentMethod.Vipps)) {
                                    PaymentCard(
                                        selected = paymentMethod == VioCheckoutOverlayController.PaymentMethod.Vipps,
                                        enabled = true,
                                        iconRes = R.drawable.ic_vipps,
                                        title = "Vipps",
                                        onClick = {
                                            paymentMethod =
                                                VioCheckoutOverlayController.PaymentMethod.Vipps; overlay.selectPaymentMethod(
                                            VioCheckoutOverlayController.PaymentMethod.Vipps
                                        )
                                        },
                                    )
                                }

                                HorizontalDivider()
                                Text(
                                    "Discount Code",
                                    style = VioTypography.title3.toComposeTextStyle(),
                                    color = dsColors.textPrimary
                                )
                                var discount by remember { mutableStateOf("") }
                                var discountMsg by remember { mutableStateOf("") }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = discount,
                                        onValueChange = { discount = it },
                                        label = { Text("Enter discount code") },
                                        modifier = Modifier.weight(1f),
                                    )
                                    Button(onClick = {
                                        scope.launch {
                                            val ok = cartManager.discountApplyOrCreate(discount)
                                            discountMsg =
                                                if (ok) "Discount applied" else cartManager.errorMessage
                                                    ?: "Cannot apply discount"
                                            itemsState = cartManager.items
                                            shippingTotalState = cartManager.shippingTotal
                                            cartTotalState = cartManager.cartTotal
                                        }
                                    }) { Text("Apply") }
                                }
                                if (discountMsg.isNotBlank()) {
                                    Text(
                                        discountMsg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()
                                Text(
                                    "Order Summary",
                                    style = VioTypography.title3.toComposeTextStyle(),
                                    color = dsColors.textPrimary
                                )
                                 Row(
                                     Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Text(
                                         "Subtotal",
                                         color = dsColors.textSecondary
                                     ); Text(
                                     "${cartManager.currency} ${
                                         String.format(
                                             "%.2f",
                                             subtotalState
                                         )
                                     }", color = dsColors.textPrimary
                                 )
                                 }
                                 Row(
                                     Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Text(
                                         "Shipping",
                                         color = dsColors.textSecondary
                                     ); Text(
                                     if (shippingTotalState == 0.0) "Free" else "${cartManager.currency} ${
                                         String.format(
                                             "%.2f",
                                             shippingTotalState
                                         )
                                     }", color = dsColors.textPrimary
                                 )
                                 }
                                     Row(
                                         Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text(
                                             "Tax",
                                             color = dsColors.textSecondary
                                         ); Text(
                                         "${cartManager.currency} ${
                                             String.format(
                                                 "%.2f",
                                                 taxTotalState
                                             )
                                         }", color = dsColors.textPrimary
                                     )
                                     }
                                 Row(
                                     Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Text("Discount", color = dsColors.textSecondary)
                                     Text(
                                         "${cartManager.currency} ${
                                             String.format(
                                                 "%.2f",
                                                 -discountTotalState
                                             )
                                         }",
                                         color = if (discountTotalState > 0) Color(0xFF2E7D32) else dsColors.textPrimary
                                     )
                                 }
                                 Row(
                                     Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Text(
                                         "Total",
                                         style = MaterialTheme.typography.titleMedium,
                                         fontWeight = FontWeight.Bold,
                                         color = dsColors.textPrimary
                                     )
                                     Text(
                                         "${cartManager.currency} ${
                                             String.format(
                                                 "%.2f",
                                                 cartTotalState
                                             )
                                         }",
                                         style = MaterialTheme.typography.titleMedium,
                                         fontWeight = FontWeight.Bold,
                                         color = MaterialTheme.colorScheme.primary
                                     )
                                 }

                                var isPaymentProcessing by remember { mutableStateOf(false) }

                                // Bottom CTA
                                VioButton(
                                    model = VioButtonModel(
                                        title = "Initiate Payment",
                                        style = VioButtonModel.Style.Primary,
                                        size = VioButtonModel.Size.Large,
                                        isLoading = isPaymentProcessing,
                                        onClick = {
                                            if (!isPaymentProcessing) {
                                                isPaymentProcessing = true
                                                // Ensure checkout is created/updated, then trigger provider
                                            overlay.proceedToPayment(advanceToReview = false) { _ ->
                                                overlay.updateCheckout(
                                                    paymentMethod,
                                                    advanceToSuccess = false
                                                ) { _ ->
                                                    when (paymentMethod) {
                                                        VioCheckoutOverlayController.PaymentMethod.Stripe -> {
                                                            overlay.requestStripeIntent { res ->
                                                                res.onSuccess { dto ->
                                                                    if (dto != null && dto.clientSecret.isNotBlank() && dto.publishableKey.isNotBlank()) {
                                                                        PaymentSheetBridge.ensureConfigured(
                                                                            dto.publishableKey
                                                                        )
                                                                        if (!PaymentSheetBridge.isReady()) {
                                                                            isPaymentProcessing = false
                                                                            ToastManager.showError("Cannot open Stripe: bridge not ready")
                                                                            return@onSuccess
                                                                        }
                                                                        val customerId =
                                                                            dto.customer
                                                                        val ephemeral =
                                                                            dto.ephemeralKey
                                                                        val customerCfg =
                                                                            if (!customerId.isNullOrBlank() && !ephemeral.isNullOrBlank()) {
                                                                                PaymentSheet.CustomerConfiguration(
                                                                                    customerId,
                                                                                    ephemeral
                                                                                )
                                                                            } else null
                                                                        val config =
                                                                            PaymentSheet.Configuration(
                                                                                merchantDisplayName = "Vio",
                                                                                customer = customerCfg,
                                                                            )
                                                                        PaymentSheetBridge.onResult =
                                                                            { result ->
                                                                                when (result) {
                                                                                    is PaymentSheetResult.Completed -> {
                                                                                        ToastManager.showSuccess(
                                                                                            "Payment successful"
                                                                                        )
                                                                                        overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Processing)
                                                                                        overlay.updateCheckout(
                                                                                            paymentMethod = VioCheckoutOverlayController.PaymentMethod.Stripe,
                                                                                            advanceToSuccess = true,
                                                                                            status = "paid",
                                                                                        ) { r ->
                                                                                            r.onSuccess {
                                                                                                scope.launch { cartManager.clearCart() }
                                                                                                overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Success)
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    is PaymentSheetResult.Canceled -> {
                                                                                        isPaymentProcessing = false
                                                                                        ToastManager.showInfo(
                                                                                            "Payment canceled"
                                                                                        )
                                                                                    }

                                                                                    is PaymentSheetResult.Failed -> {
                                                                                        isPaymentProcessing = false
                                                                                        ToastManager.showError(
                                                                                            "Payment failed"
                                                                                        )
                                                                                    }
                                                                                    else -> {}
                                                                                }
                                                                            }
                                                                        PaymentSheetBridge.presentPaymentIntent(
                                                                            dto.clientSecret,
                                                                            config,
                                                                        )
                                                                    } else {
                                                                        isPaymentProcessing = false
                                                                        ToastManager.showError("Stripe intent failed")
                                                                    }
                                                                }
                                                                res.onFailure {
                                                                    isPaymentProcessing = false
                                                                    ToastManager.showError(
                                                                        "Stripe intent error"
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        VioCheckoutOverlayController.PaymentMethod.Klarna -> {
                                                            val mode =
                                                                VioConfiguration.shared.state.value.cart.klarnaMode
                                                            if (mode == KlarnaMode.NATIVE) {
                                                                // Build init payload mirroring Swift (customer + billing/shipping from draft)
                                                                val iso2 =
                                                                    draft.resolveISO2(cartManager.country)
                                                                // Update phoneCountryCode from cart manager
                                                                draft.phoneCountryCode =
                                                                    cartManager.phoneCode.replace(
                                                                        "+",
                                                                        ""
                                                                    )
                                                                val customerDto =
                                                                    io.reachu.sdk.domain.models.KlarnaNativeCustomerInputDto(
                                                                        email = draft.email.ifBlank { null },
                                                                        phone = draft.resolveFullPhoneNumber(
                                                                            iso2
                                                                        ).ifBlank { null },
                                                                        dob = null,
                                                                        type = null,
                                                                        organizationRegistrationId = null,
                                                                    )
                                                                val addr = { isBilling: Boolean ->
                                                                    io.reachu.sdk.domain.models.KlarnaNativeAddressInputDto(
                                                                        givenName = draft.firstName.ifBlank { null },
                                                                        familyName = draft.lastName.ifBlank { null },
                                                                        email = draft.email.ifBlank { null },
                                                                        phone = draft.resolveFullPhoneNumber(
                                                                            iso2
                                                                        ).ifBlank { null },
                                                                        streetAddress = draft.address1.ifBlank { null },
                                                                        streetAddress2 = draft.address2.ifBlank { null },
                                                                        city = draft.city.ifBlank { null },
                                                                        region = draft.province.ifBlank { null },
                                                                        postalCode = draft.zip.ifBlank { null },
                                                                        country = iso2.ifBlank { null },
                                                                    )
                                                                }
                                                                val initBilling = addr(true)
                                                                val initShipping = addr(false)
                                                                val input =
                                                                    io.reachu.sdk.domain.models.KlarnaNativeInitInputDto(
                                                                        countryCode = cartManager.country,
                                                                        currency = cartManager.currency,
                                                                        locale = draft.resolveLocale(
                                                                            iso2
                                                                        ),
                                                                        returnUrl = "https://httpbin.org/status/200",
                                                                        intent = "buy",
                                                                        autoCapture = true,
                                                                        customer = customerDto,
                                                                        billingAddress = initBilling,
                                                                        shippingAddress = initShipping,
                                                                    )
                                                                overlay.initKlarnaNative(input) { res ->
                                                                    res.onSuccess { dto ->
                                                                        val clientToken =
                                                                            dto?.clientToken
                                                                        val categories =
                                                                            dto?.paymentMethodCategories?.mapNotNull { it.identifier }
                                                                                ?: emptyList()
                                                                        val category = when {
                                                                            categories.contains("pay_now") -> "pay_now"
                                                                            categories.contains("pay_later") -> "pay_later"
                                                                            categories.contains("slice_it") -> "slice_it"
                                                                            categories.contains("pay_over_time") -> "pay_over_time"
                                                                            categories.isNotEmpty() -> categories.first()
                                                                            else -> null
                                                                        }
                                                                         if (!clientToken.isNullOrBlank() && category != null && io.reachu.VioUI.KlarnaBridge.isReady()) {
                                                                             io.reachu.VioUI.KlarnaBridge.present(
                                                                                 clientToken = clientToken,
                                                                                 category = category,
                                                                                 autoAuthorize = true,
                                                                                 onAuthorized = { authToken ->
                                                                                    // Reuse same customer/addresses on confirm to mirror Swift
                                                                                    overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Processing)
                                                                                    overlay.confirmKlarnaNative(
                                                                                        authorizationToken = authToken,
                                                                                        autoCapture = true,
                                                                                        customer = customerDto,
                                                                                        billingAddress = initBilling,
                                                                                        shippingAddress = initShipping,
                                                                                    ) { confirmRes ->
                                                                                        confirmRes.onSuccess {
                                                                                            overlay.updateCheckout(
                                                                                                paymentMethod = VioCheckoutOverlayController.PaymentMethod.Klarna,
                                                                                                advanceToSuccess = true,
                                                                                                status = "paid",
                                                                                            ) { r ->
                                                                                                r.onSuccess {
                                                                                                    scope.launch { cartManager.resetCartAndCreateNew() }
                                                                                                    overlay.goToStep(VioCheckoutOverlayController.CheckoutStep.Success)
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        confirmRes.onFailure {
                                                                                            io.reachu.VioUI.Managers.ToastManager.showError(
                                                                                                "Klarna confirm error"
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                },
                                                                                onCancel = {
                                                                                    isPaymentProcessing = false
                                                                                    io.reachu.VioUI.Managers.ToastManager.showInfo(
                                                                                        "Payment canceled"
                                                                                    )
                                                                                },
                                                                                onError = { _ ->
                                                                                    // Fallback to web if SDK presentation fails
                                                                                    overlay.initKlarna(
                                                                                        countryCode = cartManager.country,
                                                                                        href = draft.successUrl.takeIf {
                                                                                            it.startsWith(
                                                                                                "http",
                                                                                                true
                                                                                            )
                                                                                        }
                                                                                            ?: "https://example.com/",
                                                                                        email = draft.email,
                                                                                    ) { webRes ->
                                                                                        webRes.onSuccess { webDto ->
                                                                                            val url =
                                                                                                webDto?.checkoutUrl
                                                                                            val snippet =
                                                                                                webDto?.htmlSnippet
                                                                                            when {
                                                                                                !url.isNullOrBlank() -> {
                                                                                                    isPaymentProcessing = false
                                                                                                    runCatching {
                                                                                                        context.startActivity(
                                                                                                            Intent(
                                                                                                                Intent.ACTION_VIEW,
                                                                                                                Uri.parse(
                                                                                                                    url
                                                                                                                )
                                                                                                            )
                                                                                                        )
                                                                                                    }
                                                                                                        .onFailure {
                                                                                                            ToastManager.showError(
                                                                                                                "Cannot open Klarna URL"
                                                                                                            )
                                                                                                        }
                                                                                                }

                                                                                                !snippet.isNullOrBlank() -> {
                                                                                                    val i =
                                                                                                        Intent(
                                                                                                            context,
                                                                                                            io.reachu.VioUI.KlarnaWebActivity::class.java
                                                                                                        )
                                                                                                    i.putExtra(
                                                                                                        "html_snippet",
                                                                                                        snippet
                                                                                                    )
                                                                                                    i.putExtra(
                                                                                                        "success_url",
                                                                                                        draft.successUrl.takeIf {
                                                                                                            it.startsWith(
                                                                                                                "http",
                                                                                                                true
                                                                                                            )
                                                                                                        }
                                                                                                            ?: "https://example.com/success")
                                                                                                    isPaymentProcessing = false
                                                                                                    runCatching {
                                                                                                        context.startActivity(
                                                                                                            i
                                                                                                        )
                                                                                                    }
                                                                                                        .onFailure {
                                                                                                            ToastManager.showError(
                                                                                                                "Cannot open Klarna WebView"
                                                                                                            )
                                                                                                        }
                                                                                                }

                                                                                                else -> ToastManager.showError(
                                                                                                    "Klarna init failed"
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                        webRes.onFailure {
                                                                                            ToastManager.showError(
                                                                                                "Klarna init error"
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                            )
                                                                        } else {
                                                                            // Fallback to web if no client token, bridge not ready, or pay_now not available
                                                                            if (category == null) ToastManager.showInfo(
                                                                                "Klarna Pay Now not available; opening web checkout"
                                                                            )
                                                                            overlay.initKlarna(
                                                                                countryCode = cartManager.country,
                                                                                href = draft.successUrl.takeIf {
                                                                                    it.startsWith(
                                                                                        "http",
                                                                                        true
                                                                                    )
                                                                                }
                                                                                    ?: "https://example.com/",
                                                                                email = draft.email,
                                                                            ) { webRes ->
                                                                                webRes.onSuccess { webDto ->
                                                                                    val url =
                                                                                        webDto?.checkoutUrl
                                                                                    val snippet =
                                                                                        webDto?.htmlSnippet
                                                                                    when {
                                                                                        !url.isNullOrBlank() -> {
                                                                                            runCatching {
                                                                                                context.startActivity(
                                                                                                    Intent(
                                                                                                        Intent.ACTION_VIEW,
                                                                                                        Uri.parse(
                                                                                                            url
                                                                                                        )
                                                                                                    )
                                                                                                )
                                                                                            }
                                                                                                .onFailure {
                                                                                                    ToastManager.showError(
                                                                                                        "Cannot open Klarna URL"
                                                                                                    )
                                                                                                }
                                                                                        }

                                                                                        !snippet.isNullOrBlank() -> {
                                                                                            val i =
                                                                                                Intent(
                                                                                                    context,
                                                                                                    io.reachu.VioUI.KlarnaWebActivity::class.java
                                                                                                )
                                                                                            i.putExtra(
                                                                                                "html_snippet",
                                                                                                snippet
                                                                                            )
                                                                                            i.putExtra(
                                                                                                "success_url",
                                                                                                draft.successUrl.takeIf {
                                                                                                    it.startsWith(
                                                                                                        "http",
                                                                                                        true
                                                                                                    )
                                                                                                }
                                                                                                    ?: "https://example.com/success")
                                                                                            runCatching {
                                                                                                context.startActivity(
                                                                                                    i
                                                                                                )
                                                                                            }
                                                                                                .onFailure {
                                                                                                    ToastManager.showError(
                                                                                                        "Cannot open Klarna WebView"
                                                                                                    )
                                                                                                }
                                                                                        }

                                                                                        else -> {
                                                                                            isPaymentProcessing = false
                                                                                            ToastManager.showError(
                                                                                                "Klarna init failed"
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                                webRes.onFailure {
                                                                                    isPaymentProcessing = false
                                                                                    ToastManager.showError(
                                                                                        "Klarna init error"
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    res.onFailure {
                                                                        isPaymentProcessing = false
                                                                        ToastManager.showError(
                                                                            "Klarna native init error"
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                // Web mode
                                                                overlay.initKlarna(
                                                                    countryCode = cartManager.country,
                                                                    href = draft.successUrl.takeIf {
                                                                        it.startsWith(
                                                                            "http",
                                                                            true
                                                                        )
                                                                    } ?: "https://example.com/",
                                                                    email = draft.email,
                                                                ) { res ->
                                                                    res.onSuccess { dto ->
                                                                        val url = dto?.checkoutUrl
                                                                        val snippet =
                                                                            dto?.htmlSnippet
                                                                        when {
                                                                            !url.isNullOrBlank() -> {
                                                                                isPaymentProcessing = false
                                                                                runCatching {
                                                                                    context.startActivity(
                                                                                        Intent(
                                                                                            Intent.ACTION_VIEW,
                                                                                            Uri.parse(
                                                                                                url
                                                                                            )
                                                                                        )
                                                                                    )
                                                                                }
                                                                                    .onFailure {
                                                                                        ToastManager.showError(
                                                                                            "Cannot open Klarna URL"
                                                                                        )
                                                                                    }
                                                                            }

                                                                            !snippet.isNullOrBlank() -> {
                                                                                val i = Intent(
                                                                                    context,
                                                                                    io.reachu.VioUI.KlarnaWebActivity::class.java
                                                                                )
                                                                                i.putExtra(
                                                                                    "html_snippet",
                                                                                    snippet
                                                                                )
                                                                                i.putExtra(
                                                                                    "success_url",
                                                                                    draft.successUrl.takeIf {
                                                                                        it.startsWith(
                                                                                            "http",
                                                                                            true
                                                                                        )
                                                                                    }
                                                                                        ?: "https://example.com/success")
                                                                                runCatching {
                                                                                    context.startActivity(
                                                                                        i
                                                                                    )
                                                                                }
                                                                                    .onFailure {
                                                                                        ToastManager.showError(
                                                                                            "Cannot open Klarna WebView"
                                                                                        )
                                                                                    }
                                                                            }

                                                                            else -> {
                                                                                isPaymentProcessing = false
                                                                                ToastManager.showError(
                                                                                    "Klarna init failed"
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    res.onFailure {
                                                                        isPaymentProcessing = false
                                                                        ToastManager.showError(
                                                                            "Klarna init error"
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        VioCheckoutOverlayController.PaymentMethod.Vipps -> {
                                                            // Ensure returnUrl uses the app scheme so the browser doesn't open an extra tab
                                                            val returnUrl =
                                                                draft.successUrl.ifBlank { "vio-demo://checkout/success" }
                                                            val emailUse =
                                                                draft.email.ifBlank { "demo@vio.live" }
                                                            scope.launch {
                                                                val dto = cartManager.vippsInit(
                                                                    email = emailUse,
                                                                    returnUrl = returnUrl
                                                                )
                                                                val url = dto?.paymentUrl
                                                                if (!url.isNullOrBlank()) {
                                                                    cartManager.checkoutId?.takeIf { it.isNotBlank() }
                                                                        ?.let {
                                                                            VippsPaymentHandler.startPaymentTracking(
                                                                                it
                                                                            )
                                                                        }
                                                                    isPaymentProcessing = false
                                                                    runCatching {
                                                                        context.startActivity(
                                                                            Intent(
                                                                                Intent.ACTION_VIEW,
                                                                                Uri.parse(url)
                                                                            )
                                                                        )
                                                                    }
                                                                        .onFailure {
                                                                            VippsPaymentHandler.stopPaymentTracking()
                                                                            isPaymentProcessing = false
                                                                            ToastManager.showError("Cannot open Vipps URL")
                                                                        }
                                                                } else {
                                                                    isPaymentProcessing = false
                                                                    ToastManager.showError("Vipps init failed")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    VioCheckoutOverlayController.CheckoutStep.Success -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = VioColors.success.toColor()
                                        .copy(alpha = 0.12f)
                                ), shape = RoundedCornerShape(VioBorderRadius.circle.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = VioColors.success.toColor()
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Payment Successful",
                                        style = VioTypography.title3.toComposeTextStyle(),
                                        color = VioColors.success.toColor()
                                    )
                                }
                            }
                            Text(
                                "Your order is confirmed.",
                                style = VioTypography.body.toComposeTextStyle(),
                                color = dsColors.textSecondary
                            )
                            VioButton(
                                model = VioButtonModel(
                                    title = "Done",
                                    style = VioButtonModel.Style.Primary,
                                    size = VioButtonModel.Size.Medium,
                                    onClick = { onBack() }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    VioCheckoutOverlayController.CheckoutStep.Error -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Something went wrong",
                                style = VioTypography.title3.toComposeTextStyle(),
                                color = VioColors.error.toColor()
                            )
                            VioButton(
                                model = VioButtonModel(
                                    title = "Back",
                                    style = VioButtonModel.Style.Tertiary,
                                    size = VioButtonModel.Size.Medium,
                                    onClick = {
                                        overlay.goToPreviousStep()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    else -> Unit
                }

                if (vippsInProgress) {
                    VippsProcessingBanner(status = vippsStatus)
                }
            }
                if (vippsInProgress) {
                    VippsProcessingBanner(status = vippsStatus)
                }
            }
        }
    }
}
