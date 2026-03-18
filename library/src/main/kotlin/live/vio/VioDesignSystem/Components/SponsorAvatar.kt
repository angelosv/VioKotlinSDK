package live.vio.VioDesignSystem.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.vio.VioDesignSystem.SponsorAssets

/**
 * A circular avatar component for sponsors.
 * Displays the sponsor's avatar image or falls back to initials.
 */
@Composable
fun SponsorAvatar(
    sponsor: SponsorAssets,
    modifier: Modifier = Modifier,
    size: Int = 32
) {
    val initials = getInitials(sponsor.name)
    val backgroundColor = Color(android.graphics.Color.parseColor(
        if (!sponsor.primaryColor.startsWith("#")) "#${sponsor.primaryColor}" else sponsor.primaryColor
    ))

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (!sponsor.avatarUrl.isNullOrBlank()) {
            CachedAsyncImage(
                url = sponsor.avatarUrl,
                contentDescription = sponsor.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                color = sponsor.textOnPrimary,
                fontSize = (size / 2.5).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getInitials(name: String): String {
    if (name.isBlank()) return ""
    val parts = name.trim().split(" ")
    return if (parts.size >= 2) {
        "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
    } else {
        name.take(2).uppercase()
    }
}
