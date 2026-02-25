package com.reachu.viaplaydemo.ui.model

import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slug: String,
)

data class ContentItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String?,
    val imageUrl: String,
    val category: String,
    val isLive: Boolean = false,
    val duration: String? = null,
    val date: String? = null,
    val homeTeamLogo: String? = null,
    val awayTeamLogo: String? = null,
    val matchTime: String? = null,
    val matchday: String? = null,
) {
    val isMatchCard: Boolean get() = homeTeamLogo != null && awayTeamLogo != null
}

object ContentMockData {
    val categories = listOf(
        Category(name = "Sporten", slug = "sporten"),
        Category(name = "Fotball", slug = "fotball"),
        Category(name = "Norsk", slug = "norsk"),
        Category(name = "Tennis", slug = "tennis"),
        Category(name = "Håndball", slug = "handball"),
        Category(name = "Sykkel", slug = "cycling"),
    )

    val items = listOf(
        ContentItem(
            title = "Barcelona - PSG",
            subtitle = "Fotball • Menn • UEFA Champions League",
            imageUrl = "barcelona_psg_bg",
            category = "Fotball",
            isLive = true,
            date = "Tir. 18:40",
            homeTeamLogo = "barcelona_logo",
            awayTeamLogo = "psg_logo",
            matchTime = "18:40",
            matchday = "M",
        ),
        ContentItem(
            title = "FOTBALLKVELD",
            subtitle = "Alt fra CL-runden",
            imageUrl = "bg-card-1",
            category = "Fotball",
            isLive = false,
            date = "I dag 17:40",
        ),
        ContentItem(
            title = "CHAMPIONS LEAGUE",
            subtitle = "Kremmerne",
            imageUrl = "bg-card-3",
            category = "Fotball",
            isLive = false,
            date = "I dag 19:00",
        ),
        ContentItem(
            title = "Rolex Shanghai Masters",
            subtitle = "Dag 2",
            imageUrl = "bg-card-2",
            category = "Tennis",
            isLive = true,
            duration = "DIREKTE",
        ),
        ContentItem(
            title = "Rosenborg vs Brann",
            subtitle = "Fotball kveld",
            imageUrl = "bg-card-1",
            category = "Fotball",
            isLive = false,
            date = "I dag 17:40",
        ),
        ContentItem(
            title = "Håndball Highlights",
            subtitle = "Best of Champions League",
            imageUrl = "bg-card-2",
            category = "Håndball",
            isLive = false,
            date = "I går 20:00",
        ),
        ContentItem(
            title = "Sykkel VM",
            subtitle = "Herrenes fellesstart",
            imageUrl = "bg-card-3",
            category = "Sykkel",
            isLive = false,
            date = "27 sep",
        ),
    )
}

enum class TabItem(val label: String) {
    HOME("Hjem"),
    SPORT("Sport"),
    STREAMS("Direkte"),
    STORE("Butikk"),
    PROFILE("Profil"),
}
