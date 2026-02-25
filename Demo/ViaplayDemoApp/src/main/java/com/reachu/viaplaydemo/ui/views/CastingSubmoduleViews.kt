package com.reachu.viaplaydemo.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reachu.viaplaydemo.ui.model.*
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun CastingLineupView(lineup: CastingTeamLineup) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF1E1E2C), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Lineup Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Viaplay Logo placeholder (Red pill)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFE91052), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Viaplay Oppstilling", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                    }
                    Text("${lineup.teamName} • ${lineup.formation}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Sponset av", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ELKJØP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Button
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Liste", color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Football Pitch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        ) {
            FootballPitch(modifier = Modifier.fillMaxSize())
            
            // Players
            lineup.players.forEach { player ->
                PlayerNode(
                    player = player,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reaction Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReactionItem(Icons.Default.Whatshot, "398", Color(0xFFFF5722))
            ReactionItem(Icons.Default.Favorite, "164", Color(0xFFE91E63))
            ReactionItem(Icons.Default.SportsSoccer, "342", Color.White)
            ReactionItem(Icons.Default.EmojiEvents, "170", Color(0xFFFFC107))
            ReactionItem(Icons.Default.ThumbUp, "268", Color(0xFFFFC107))
            ReactionItem(Icons.Default.AdsClick, "96", Color(0xFFE91E63))
        }
    }
}

@Composable
fun FootballPitch(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFF233030))) {
        val w = size.width
        val h = size.height
        val stroke = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.3f)

        // Outer margin
        val m = 10.dp.toPx()
        
        // Pitch boundary
        drawRect(color, Offset(m, m), Size(w - 2 * m, h - 2 * m), style = Stroke(stroke))
        
        // Halfway line
        drawLine(color, Offset(m, h / 2), Offset(w - m, h / 2), stroke)
        
        // Center circle
        drawCircle(color, 40.dp.toPx(), center = Offset(w / 2, h / 2), style = Stroke(stroke))
        drawCircle(color, 2.dp.toPx(), center = Offset(w / 2, h / 2))

        // Penalty areas (Top and Bottom)
        // Top
        drawRect(color, Offset(w * 0.25f, m), Size(w * 0.5f, h * 0.15f), style = Stroke(stroke))
        drawRect(color, Offset(w * 0.35f, m), Size(w * 0.3f, h * 0.05f), style = Stroke(stroke))
        
        // Bottom
        drawRect(color, Offset(w * 0.25f, h - m - h * 0.15f), Size(w * 0.5f, h * 0.15f), style = Stroke(stroke))
        drawRect(color, Offset(w * 0.35f, h - m - h * 0.05f), Size(w * 0.3f, h * 0.05f), style = Stroke(stroke))
    }
}

@Composable
fun PlayerNode(player: CastingLineupPlayer, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val px = player.x * maxWidth.value
        val py = player.y * maxHeight.value
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = px.dp - 24.dp, y = py.dp - 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF0091EA), CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = player.number.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(60.dp)
            )
        }
    }
}

@Composable
fun ReactionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(count, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun CastingStatisticsView(stats: List<CastingStatistic>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(stats) { stat ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stat.name,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (stat.unit == "%") "${stat.homeValue}%" else stat.homeValue.toInt().toString(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.End
                    )
                    
                    val total = stat.homeValue + stat.awayValue
                    val homeWeight = (if (total > 0) stat.homeValue / total else 0.5f).coerceIn(0.01f, 0.99f)
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(homeWeight)
                                .background(Color(0xFFE91052))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f - homeWeight)
                                .background(Color.Transparent)
                        )
                    }
                    
                    Text(
                        text = if (stat.unit == "%") "${stat.awayValue}%" else stat.awayValue.toInt().toString(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
fun CastingChatView(messages: List<CastingChatMessage>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).background(message.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(message.username.take(1), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(message.username, color = message.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(message.timestamp, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    }
                    Text(message.text, color = Color.White, fontSize = 14.sp)
                    if (message.likes > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
                            Text(message.likes.toString(), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CastingStandingsView(standings: List<CastingStanding>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Premier League 2024/25", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("#", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.width(20.dp))
            Text("LAG", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("S", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            Text("±", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            Text("P", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(standings) { team ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(team.rank.toString(), color = Color.White, fontSize = 14.sp, modifier = Modifier.width(20.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(20.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) // Logo placeholder
                        Text(team.teamName, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(team.played.toString(), color = Color.White, fontSize = 14.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text(team.gd.toString(), color = if (team.gd >= 0) Color.Green else Color.Red, fontSize = 14.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text(team.points.toString(), color = Color.White, fontSize = 14.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CastingLiveResultsView(matches: List<CastingLiveMatch>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Live Resultater",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            val liveCount = matches.count { it.isLive }
            if (liveCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                    Text("$liveCount LIVE", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(matches) { match ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(match.homeTeam, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(match.homeScore.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                                Text(match.awayScore.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Text(match.awayTeam, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(match.competition, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (match.isLive) {
                                    Box(modifier = Modifier.size(4.dp).background(Color.Red, CircleShape))
                                    Text(match.status, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(match.status, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CastingInteraktivtView(
    liveContent: @Composable () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // First show any "Live" content pushed from SDK
        item {
            liveContent()
        }
        
        // Then show the persistent Mock Content
        item {
             InteraktivtMockContent()
        }
    }
}

@Composable
fun InteraktivtMockContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header with Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Avstemminger",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("2", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        ContestCardMock()
        PollCardMock()
    }
}

@Composable
fun ContestCardMock() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF5146B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Viaplay Konkurranse", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append("Premie • ")
                                withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF00C853))) {
                                    append("Aktiv nå")
                                }
                            },
                            fontSize = 12.sp
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sponset av", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ELKJØP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Vinn en drakt fra ditt favorittlag!",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFF5146B), modifier = Modifier.size(18.dp))
                    Text("Premie: Fotballdrakt", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    Text("Trekning: Etter kampen", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Delta Button
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5146B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PollCardMock() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF9C27B0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Viaplay Avstemning", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Direktesending • 10:00", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sponset av", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ELKJØP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Hvem vinner denne kampen?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PollOptionMock("Barcelona")
                PollOptionMock("Real Madrid")
            }
        }
    }
}

@Composable
fun PollOptionMock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable { }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}
