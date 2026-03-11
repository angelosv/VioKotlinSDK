package live.vio.VioUI.Components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.models.VioPaymentMethod
import live.vio.VioDesignSystem.Tokens.VioBorderRadius
import live.vio.VioDesignSystem.Tokens.VioSpacing

/**
 * VPaymentSheet - Composable to show available payment methods based on backend configuration.
 * Mirrors the logic from Swift's VPaymentSheet.swift.
 */
@Composable
fun VPaymentSheet(
    modifier: Modifier = Modifier,
    onPaymentMethodSelected: (VioPaymentMethod) -> Unit = {}
) {
    val configState by VioConfiguration.shared.state.collectAsState()
    val checkoutConfig = configState.checkout
    val campaignConfig = configState.campaign

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp)
    ) {
        if (checkoutConfig == null) {
            // Check campaign config first if no dynamic checkout config is available
            if (campaignConfig.hasApplePay) {
                GooglePayButton(onClick = { onPaymentMethodSelected(VioPaymentMethod.APPLE_PAY) })
            }
        } else {
            // Google Pay (Mapping from Apple Pay config as per requirement)
            if (checkoutConfig.hasApplePay || campaignConfig.hasApplePay) {
                GooglePayButton(onClick = { onPaymentMethodSelected(VioPaymentMethod.APPLE_PAY) })
            }

            // Klarna
            if (checkoutConfig.hasKlarna) {
                PaymentButton(
                    text = "Klarna",
                    backgroundColor = Color(0xFFFFB3C7), // Klarna Pink
                    textColor = Color.Black,
                    onClick = { onPaymentMethodSelected(VioPaymentMethod.KLARNA) }
                )
            }

            // Vipps
            if (checkoutConfig.hasVipps) {
                PaymentButton(
                    text = "Vipps",
                    backgroundColor = Color(0xFFFF5B24), // Vipps Orange
                    textColor = Color.White,
                    onClick = { onPaymentMethodSelected(VioPaymentMethod.VIPPS) }
                )
            }
        }
    }
}

@Composable
private fun GooglePayButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(VioBorderRadius.medium.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pay with ",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            )
            Text(
                text = "Google Pay",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )
        }
    }
}

@Composable
private fun PaymentButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(VioBorderRadius.medium.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        )
    }
}
