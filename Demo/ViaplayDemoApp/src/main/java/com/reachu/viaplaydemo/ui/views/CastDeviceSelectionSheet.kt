package com.reachu.viaplaydemo.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reachu.viaplaydemo.casting.CastDevice
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun CastDeviceSelectionSheet(
    devices: List<CastDevice>,
    onDeviceSelected: (CastDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C0B2E))
            .padding(24.dp)
    ) {
        Text(
            text = "Koble til en enhet",
            style = ViaplayTheme.Typography.title,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(devices) { device ->
                DeviceItem(device = device, onClick = { onDeviceSelected(device) })
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
        
        if (devices.isEmpty()) {
            Text(
                text = "Søker etter enheter...",
                style = ViaplayTheme.Typography.body,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DeviceItem(device: CastDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(ViaplayTheme.CornerRadius.small)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = device.name,
                style = ViaplayTheme.Typography.body,
                color = Color.White
            )
            Text(
                text = "Klar til å koble til",
                style = ViaplayTheme.Typography.small,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
