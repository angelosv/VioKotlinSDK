package com.reachu.tv2demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import com.reachu.tv2demo.ui.theme.TV2Theme

@Composable
fun OfferBannerView(
    title: String = "Ukens tilbud",
    subtitle: String? = "Se denne ukes beste tilbud",
    backgroundImageUrl: String? = null,
    countdownSeconds: Long = TimeUnit.DAYS.toSeconds(2) + TimeUnit.HOURS.toSeconds(2),
    onCtaClick: () -> Unit = {},
) {
    var remaining by remember(countdownSeconds) { mutableStateOf(countdownSeconds) }

    LaunchedEffect(countdownSeconds) {
        remaining = countdownSeconds
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color.Black, RoundedCornerShape(24.dp)),
    ) {
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = backgroundImageUrl,
                contentDescription = null,
                modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF3F2B96), Color(0xFFA8C0FF)),
                        ),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.2f)),
                    ),
                    RoundedCornerShape(24.dp),
                ),
        )

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = TV2Theme.Typography.title.copy(fontWeight = FontWeight.Bold), color = Color.White)
                subtitle?.let {
                    Text(it, style = TV2Theme.Typography.small, color = Color.White.copy(alpha = 0.8f))
                }
                CountdownRow(remaining = remaining)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Opp til 30%",
                    style = TV2Theme.Typography.body.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
                Button(onClick = onCtaClick) {
                    Text("Se alle tilbud →")
                }
            }
        }
    }
}

@Composable
private fun CountdownRow(remaining: Long) {
    val days = TimeUnit.SECONDS.toDays(remaining)
    val hours = TimeUnit.SECONDS.toHours(remaining % TimeUnit.DAYS.toSeconds(1))
    val minutes = TimeUnit.SECONDS.toMinutes(remaining % TimeUnit.HOURS.toSeconds(1))
    val seconds = remaining % 60

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (days > 0) CountdownUnit(days, "dag")
        if (hours > 0 || days > 0) CountdownUnit(hours, "time")
        CountdownUnit(minutes, "min")
        CountdownUnit(seconds, "sek")
    }
}

@Composable
private fun CountdownUnit(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = String.format("%02d", value),
                style = TV2Theme.Typography.body.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
        Text(label, style = TV2Theme.Typography.small, color = Color.White.copy(alpha = 0.8f))
    }
}
