package com.reachu.tv2demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reachu.tv2demo.ui.theme.TV2Theme

@Composable
fun TV2SponsorBadge(
    logoUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "Sponsor logo",
    showText: Boolean = true,
) {
    if (logoUrl.isNullOrBlank()) return

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        if (showText) {
            Text(
                text = "Sponset av",
                style = TV2Theme.Typography.small,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        io.reachu.VioUI.Components.compose.product.VioImage(
            url = logoUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(width = 80.dp, height = 24.dp),
        )
    }
}
