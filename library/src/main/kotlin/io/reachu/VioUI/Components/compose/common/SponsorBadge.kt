package io.reachu.VioUI.Components.compose.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import androidx.compose.ui.graphics.Color

/**
 * Sponsor badge component that displays "Sponset av" text and campaign logo.
 * 
 * @param logoUrl URL of the sponsor/campaign logo
 * @param text Text to display above the logo (default: "Sponset av")
 * @param textColor Color of the text (default: Unspecified)
 * @param modifier Modifier for the badge container
 * @param imageLoader Image loader for loading the logo
 */
@Composable
fun SponsorBadge(
    logoUrl: String?,
    text: String = "Sponset av",
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
) {
    if (logoUrl.isNullOrBlank()) return

    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Text above logo
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Light,
            color = textColor,
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Campaign logo
        VioImage(
            url = logoUrl,
            contentDescription = "Sponsor logo",
            modifier = Modifier
                .width(60.dp)
                .heightIn(max = 30.dp),
            imageLoader = imageLoader,
        )
    }
}
