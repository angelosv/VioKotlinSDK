package com.reachu.viaplaydemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reachu.viaplaydemo.ui.components.BottomTabBar
import com.reachu.viaplaydemo.ui.model.TabItem
import com.reachu.viaplaydemo.ui.model.Match
import com.reachu.viaplaydemo.ui.model.MatchMocks
import com.reachu.viaplaydemo.ui.views.MatchDetailScreen
import io.reachu.VioUI.Components.compose.product.VioProductSlider
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Components.VioProductSliderLayout

@Composable
fun ViaplaySportView(
    cartManager: CartManager,
    onBack: () -> Unit,
    onTabSelected: (TabItem) -> Unit,
) {
    var selectedMatch by remember { mutableStateOf<Match?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B25))
    ) {
        if (selectedMatch != null) {
            MatchDetailScreen(
                match = selectedMatch!!,
                cartManager = cartManager,
                onBack = { selectedMatch = null }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header / Back button mock
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "< Back to Home", 
                        color = Color.White, 
                        modifier = Modifier.clickable { onBack() }
                    )
                }

                // Vis sendeskjema button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF302F3F))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Vis sendeskjema", color = Color.White, fontWeight = FontWeight.Medium)
                }

                // Vår beste sport Section
                Text(
                    "Vår beste sport",
                    fontSize = 19.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalCarouselMock(onNavigateToMatch = { selectedMatch = it })

                Spacer(modifier = Modifier.height(32.dp))

                // Live akkurat nå Section
                Text(
                    "Live akkurat nå",
                    fontSize = 19.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalLiveContentSection(onNavigateToMatch = { selectedMatch = it })

                Spacer(modifier = Modifier.height(32.dp))

                // De beste klippene akkurat nå Section
                Text(
                    "De beste klippene akkurat nå",
                    fontSize = 19.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalClipsSection(onNavigateToMatch = { selectedMatch = MatchMocks.barcelonaVsPsg })

                // Populær sport
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Populær sport",
                    fontSize = 17.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalPopularSportSection()
                
                // Products Section
                Spacer(modifier = Modifier.height(32.dp))
                VioProductSlider(
                    cartManager = cartManager,
                    title = "Ukens tilbud",
                    layout = VioProductSliderLayout.CARDS,
                )

                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Bottom Navigation
        BottomTabBar(
            selected = TabItem.SPORT,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }
}

@Composable
fun HorizontalCarouselMock(onNavigateToMatch: (Match) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val matches = listOf(
            MatchMocks.realMadridVsBarcelona,
            MatchMocks.dortmundVsAthletic,
            MatchMocks.barcelonaVsPsg,
        )
        
        matches.forEach { match ->
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2D36))
                    .clickable { onNavigateToMatch(match) }
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Gray)) {
                        AsyncImage(
                            model = match.backgroundImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            "TONIGHT 20:00", 
                            Modifier.padding(12.dp).background(Color.White, RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp), 
                            color = Color.Black, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)) {
                            Text(
                                "LIGAEN",
                                Modifier.background(Color(0xFF2C2D36), RoundedCornerShape(5.dp)).padding(horizontal=8.dp, vertical=4.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(match.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text(match.subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalLiveContentSection(onNavigateToMatch: (Match) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(310.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2C2D36))
                .clickable { onNavigateToMatch(MatchMocks.barcelonaVsPsg) }
        ) {
            val match = MatchMocks.barcelonaVsPsg
            AsyncImage(
               model = match.backgroundImage,
               contentDescription = null,
               contentScale = ContentScale.Crop,
               modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)))

            // LIVE Badge
            Text("LIVE", Modifier.padding(12.dp).background(Color(0xFFF5146B), RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            // Progress bar
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 12.dp)) {
                 Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                     Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFF5146B), RoundedCornerShape(1.5.dp)))
                     Box(modifier = Modifier.weight(0.75f).fillMaxHeight().background(Color.White.copy(alpha=0.3f), RoundedCornerShape(1.5.dp)))
                 }
            }
        }
        
        Box(
            modifier = Modifier
                .width(310.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2C2D36))
                .clickable { onNavigateToMatch(MatchMocks.dortmundVsAthletic) }
        ) {
            val match = MatchMocks.dortmundVsAthletic
            AsyncImage(
                model = match.backgroundImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)))
            // LIVE Badge
            Text("LIVE", Modifier.padding(12.dp).background(Color(0xFFF5146B), RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            Column(
                modifier = Modifier.fillMaxSize().padding(top=30.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Star, contentDescription=null, tint=Color.Yellow, modifier=Modifier.size(16.dp))
                    Column {
                        Text("CHALLENGE", color = Color.White, fontSize = 14.sp)
                        Text("TOUR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rolex Grand", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("European Challenge", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            
            Text("15:00", Modifier.align(Alignment.BottomStart).padding(bottom=24.dp, start=12.dp), color = Color.White, fontSize = 13.sp)
            
            // Progress bar
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 12.dp)) {
                 Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                     Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFF5146B), RoundedCornerShape(1.5.dp)))
                     Box(modifier = Modifier.weight(0.75f).fillMaxHeight().background(Color.White.copy(alpha=0.3f), RoundedCornerShape(1.5.dp)))
                 }
            }
        }
    }
}

@Composable
fun HorizontalClipsSection(onNavigateToMatch: () -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Column(modifier = Modifier.width(200.dp).clickable(onClick = onNavigateToMatch)) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray)) {
                    AsyncImage(
                        model = MatchMocks.barcelonaVsPsg.backgroundImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text("00:51", Modifier.padding(8.dp).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(4.dp)).padding(horizontal=8.dp, vertical=4.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Box(modifier = Modifier.size(46.dp).background(Color.Black.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                             Icon(Icons.Filled.EmojiEvents, contentDescription=null, tint=Color.White, modifier=Modifier.size(24.dp))
                         }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("PREMIER LEAGUE", color = Color.White.copy(alpha=0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("Haaland ofret sitt for å redde City-poeng", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
        }
    }
}

@Composable
fun HorizontalPopularSportSection() {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val images = listOf(
            "https://images.unsplash.com/photo-1518063319789-7217e6706b04?auto=format&w=600&q=80",
            "https://images.unsplash.com/photo-1542144582-1ba00456b5e3?auto=format&w=600&q=80",
            "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?auto=format&w=600&q=80"
        )
        
        images.forEach { imgUrl ->
            Box(
                 modifier = Modifier
                     .width(120.dp)
                     .height(200.dp)
                     .clip(RoundedCornerShape(10.dp))
            ) {
                 AsyncImage(
                     model = imgUrl,
                     contentDescription = null,
                     contentScale = ContentScale.Crop,
                     modifier = Modifier.fillMaxSize()
                 )
            }
        }
    }
}
