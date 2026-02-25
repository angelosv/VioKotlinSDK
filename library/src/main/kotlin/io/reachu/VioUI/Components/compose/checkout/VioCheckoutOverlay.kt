package io.reachu.VioUI.Components.compose.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioCore.configuration.TypographyToken
import io.reachu.VioUI.Components.CheckoutDraft
import io.reachu.VioUI.Components.VioCheckoutOverlayController
import io.reachu.VioUI.Managers.CartItem
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.ToastManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import io.reachu.VioUI.Components.compose.utils.toVioColor

private fun String.toColor(): Color = toVioColor()

@Composable
fun VioCheckoutOverlay(
    cartManager: CartManager,
    controller: VioCheckoutOverlayController,
    paymentLauncher: CheckoutPaymentLauncher,
    assets: CheckoutAssets = CheckoutAssets(),
    onDismiss: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(controller.currentStep) }
    var paymentMethod by remember { mutableStateOf(controller.selectedPaymentMethod) }
    var isProcessing by remember { mutableStateOf(false) }
    var localItems by remember { mutableStateOf(cartManager.items) }
    var cartTotal by remember { mutableStateOf(cartManager.cartTotal) }

    LaunchedEffect(cartManager) {
        snapshotFlow { cartManager.items to cartManager.cartTotal }
            .collect { (items, total) ->
                localItems = items
                cartTotal = total
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onDismiss() }
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CheckoutTopBar(
                        step = currentStep,
                        onClose = onDismiss,
                        onBack = {
                            controller.goToPreviousStep()
                            currentStep = controller.currentStep
                        },
                    )
                    Divider()
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { OrderSummary(localItems) }
                        item {
                            TotalSummary(
                                cartTotal = cartTotal,
                                subtotal = cartManager.subtotal,
                                shipping = cartManager.shippingTotal,
                                discount = cartManager.discountTotal,
                                tax = cartManager.taxTotal,
                                currency = cartManager.currency
                            )
                        }
                        item {
                            PaymentMethodSelector(
                                methods = controller.allowedPaymentMethods,
                                selected = paymentMethod,
                                assets = assets,
                                onSelected = {
                                    controller.selectPaymentMethod(it)
                                    paymentMethod = controller.selectedPaymentMethod
                                },
                            )
                        }
                    }
                    Divider()
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                when (paymentMethod) {
                                    VioCheckoutOverlayController.PaymentMethod.Stripe -> controller.requestStripeIntent { result ->
                                        result.onSuccess { dto ->
                                            if (dto == null) {
                                                ToastManager.showError("Missing Stripe intent")
                                            } else {
                                                paymentLauncher.presentStripe(
                                                    StripeSheetConfig(
                                                        clientSecret = dto.clientSecret,
                                                        customerConfig = StripeSheetConfig.CustomerConfig(
                                                            id = dto.customer,
                                                            ephemeralKey = dto.ephemeralKey.orEmpty(),
                                                        ),
                                                    )
                                                ) { paymentResult ->
                                                    when (paymentResult) {
                                                        CheckoutPaymentResult.Completed -> {
                                                            controller.goToStep(VioCheckoutOverlayController.CheckoutStep.Success)
                                                            currentStep = controller.currentStep
                                                            ToastManager.showSuccess("Payment completed")
                                                        }
                                                        CheckoutPaymentResult.Canceled -> ToastManager.showInfo("Payment canceled")
                                                        CheckoutPaymentResult.Failed -> ToastManager.showError("Payment failed")
                                                    }
                                                }
                                            }
                                        }.onFailure {
                                            ToastManager.showError("Stripe intent failed: ${it.message}")
                                        }
                                        isProcessing = false
                                    }
                                    VioCheckoutOverlayController.PaymentMethod.Klarna -> {
                                        ToastManager.showInfo("Klarna integration pending")
                                        isProcessing = false
                                    }
                                    VioCheckoutOverlayController.PaymentMethod.Vipps -> {
                                        paymentLauncher.presentVipps { result ->
                                            if (result == CheckoutPaymentResult.Completed) {
                                                controller.goToStep(VioCheckoutOverlayController.CheckoutStep.Success)
                                                currentStep = controller.currentStep
                                            }
                                        }
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        enabled = localItems.isNotEmpty() && !isProcessing,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            text = when (paymentMethod) {
                                VioCheckoutOverlayController.PaymentMethod.Stripe -> "Pay with card"
                                VioCheckoutOverlayController.PaymentMethod.Klarna -> "Continue with Klarna"
                                VioCheckoutOverlayController.PaymentMethod.Vipps -> "Continue with Vipps"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutTopBar(step: VioCheckoutOverlayController.CheckoutStep, onClose: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = step != VioCheckoutOverlayController.CheckoutStep.OrderSummary) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Back")
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = when (step) {
                VioCheckoutOverlayController.CheckoutStep.OrderSummary -> "Order Summary"
                VioCheckoutOverlayController.CheckoutStep.Review -> "Review"
                VioCheckoutOverlayController.CheckoutStep.Processing -> "Processing"
                VioCheckoutOverlayController.CheckoutStep.Success -> "Success"
                VioCheckoutOverlayController.CheckoutStep.Error -> "Checkout Error"
            },
            style = VioTypography.title2.toComposeTextStyle(),
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun OrderSummary(items: List<CartItem>) {
    if (items.isEmpty()) {
        Text("Your cart is empty", color = VioColors.textSecondary.toColor())
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Items", style = VioTypography.title3.toComposeTextStyle(), color = VioColors.textPrimary.toColor())
        items.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Qty ${item.quantity}", style = MaterialTheme.typography.labelMedium, color = VioColors.textSecondary.toColor())
                    }
                    Text("${item.currency} ${String.format("%.2f", item.price * item.quantity)}")
                }
            }
        }
    }
}

@Composable
private fun TotalSummary(cartTotal: Double, subtotal: Double, shipping: Double, discount: Double, tax: Double, currency: String) {
    Column {
        Divider()
        Spacer(Modifier.height(8.dp))
        SummaryRow("Subtotal", currency, subtotal)
        SummaryRow("Shipping", currency, shipping)
        SummaryRow("Tax", currency, tax)
        SummaryRow("Discount", currency, -discount, color = if (discount > 0) VioColors.success.toColor() else VioColors.textSecondary.toColor())
        Spacer(Modifier.height(8.dp))
        Divider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$currency ${String.format("%.2f", cartTotal)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VioColors.primary.toColor())
        }
        Divider()
    }
}

@Composable
private fun SummaryRow(label: String, currency: String, amount: Double, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VioColors.textSecondary.toColor())
        Text("$currency ${String.format("%.2f", amount)}", style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun PaymentMethodSelector(
    methods: List<VioCheckoutOverlayController.PaymentMethod>,
    selected: VioCheckoutOverlayController.PaymentMethod,
    assets: CheckoutAssets,
    onSelected: (VioCheckoutOverlayController.PaymentMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Payment Method", style = VioTypography.title3.toComposeTextStyle(), color = VioColors.textPrimary.toColor())
        methods.forEach { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(method) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = when (method) {
                            VioCheckoutOverlayController.PaymentMethod.Stripe -> "Credit Card"
                            VioCheckoutOverlayController.PaymentMethod.Klarna -> "Klarna"
                            VioCheckoutOverlayController.PaymentMethod.Vipps -> "Vipps"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = VioColors.textPrimary.toColor(),
                    )
                    if (selected == method) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = VioColors.primary.toColor())
                    }
                }
            }
        }
    }
}

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
