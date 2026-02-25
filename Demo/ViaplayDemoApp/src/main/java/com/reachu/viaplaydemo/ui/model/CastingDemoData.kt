package com.reachu.viaplaydemo.ui.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

// MARK: - Models

data class CastingChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val text: String,
    val color: Color,
    val likes: Int = 0,
    val isVerified: Boolean = false,
    val timestamp: String = "15:30"
)

enum class CastingEventType {
    GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION, KICKOFF, HALFTIME, FULLTIME
}

data class CastingMatchEvent(
    val id: String = UUID.randomUUID().toString(),
    val minute: Int,
    val type: CastingEventType,
    val player: String? = null,
    val playerOut: String? = null,
    val teamName: String,
    val isHome: Boolean,
    val score: String? = null,
    val description: String? = null
)

data class CastingStatistic(
    val name: String,
    val homeValue: Float,
    val awayValue: Float,
    val unit: String? = null
)

data class CastingStanding(
    val rank: Int,
    val teamName: String,
    val teamLogo: String,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val gd: Int,
    val points: Int,
    val form: List<String> // ["W", "D", "L", "W", "W"]
)

data class CastingLineupPlayer(
    val number: Int,
    val name: String,
    val x: Float, // 0.0 to 1.0
    val y: Float  // 0.0 to 1.0 (0.0 is top/FW, 1.0 is bottom/GK)
)

data class CastingTeamLineup(
    val teamName: String,
    val formation: String,
    val players: List<CastingLineupPlayer>
)

data class CastingLiveMatch(
    val id: String = UUID.randomUUID().toString(),
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val competition: String,
    val status: String,
    val isLive: Boolean = false
)

data class CastingInteractiveEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "POLL", "CONTEST", "PRODUCT"
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null
)

// MARK: - Data Objects

object CastingDemoData {
    
    val mockChatMessages = listOf(
        CastingChatMessage(username = "SportsFan23", text = "Endelig! La oss gå! 🔥", color = Color(0xFF00BCD4), likes = 5),
        CastingChatMessage(username = "MatchMaster", text = "Dette blir en god kamp!", color = Color(0xFFFF9800), likes = 8),
        CastingChatMessage(username = "GoalKeeper", text = "Vamos Barcelona! 💪", color = Color(0xFF4CAF50)),
        CastingChatMessage(username = "TacticsGuru", text = "Interessant bytte så tidlig", color = Color(0xFF009688), likes = 3),
        CastingChatMessage(username = "PowerFan", text = "Dette er en fantastisk mulighet! 🎁", color = Color(0xFFFF5722), likes = 12),
        CastingChatMessage(username = "ChampionsFan", text = "Billetter til Champions League?! Jeg må delta! 🎫", color = Color(0xFF9C27B0), likes = 18),
        CastingChatMessage(username = "TechLover", text = "25% rabatt?! Dette må jeg sjekke ut! 📺", color = Color(0xFF00E5FF), likes = 9),
        CastingChatMessage(username = "StrikerKing", text = "Så nærme! Nesten 3-0!", color = Color(0xFFE91E63), likes = 5),
        CastingChatMessage(username = "FutbolLoco", text = "Gavi fikk gult kort", color = Color(0xFFFFEB3B), likes = 2),
        CastingChatMessage(username = "UltrasGroup", text = "Vi trenger enda et mål!", color = Color(0xFFF44336), likes = 11)
    )

    val mockHighlights = listOf(
        CastingMatchEvent(minute = 0, type = CastingEventType.KICKOFF, teamName = "Barcelona", isHome = true),
        CastingMatchEvent(minute = 5, type = CastingEventType.SUBSTITUTION, player = "A. Scott", playerOut = "T. Adams", teamName = "PSG", isHome = false),
        CastingMatchEvent(minute = 13, type = CastingEventType.GOAL, player = "A. Diallo", teamName = "Barcelona", isHome = true, score = "1-0"),
        CastingMatchEvent(minute = 18, type = CastingEventType.YELLOW_CARD, player = "Casemiro", teamName = "Barcelona", isHome = true),
        CastingMatchEvent(minute = 25, type = CastingEventType.YELLOW_CARD, player = "M. Tavernier", teamName = "PSG", isHome = false),
        CastingMatchEvent(minute = 32, type = CastingEventType.GOAL, player = "B. Mbeumo", teamName = "Barcelona", isHome = true, score = "2-0")
    )

    val mockStatistics = listOf(
        CastingStatistic(name = "Possession", homeValue = 56.3f, awayValue = 43.7f, unit = "%"),
        CastingStatistic(name = "Passes", homeValue = 105f, awayValue = 84f),
        CastingStatistic(name = "Accurate passes", homeValue = 84f, awayValue = 52f),
        CastingStatistic(name = "Offsides", homeValue = 0f, awayValue = 1f),
        CastingStatistic(name = "Shots", homeValue = 12f, awayValue = 0f),
        CastingStatistic(name = "Shots on goal", homeValue = 3f, awayValue = 0f),
        CastingStatistic(name = "Saves", homeValue = 0f, awayValue = 2f),
        CastingStatistic(name = "Corners", homeValue = 3f, awayValue = 1f)
    )

    val mockStandings = listOf(
        CastingStanding(rank = 1, teamName = "Arsenal", teamLogo = "arsenal_logo", played = 16, wins = 11, draws = 3, losses = 2, gd = 20, points = 36, form = listOf("W", "W", "D", "W", "W")),
        CastingStanding(rank = 2, teamName = "Man City", teamLogo = "city_logo", played = 16, wins = 11, draws = 1, losses = 4, gd = 22, points = 34, form = listOf("W", "W", "L", "W", "D")),
        CastingStanding(rank = 3, teamName = "Aston Villa", teamLogo = "villa_logo", played = 16, wins = 10, draws = 3, losses = 3, gd = 8, points = 33, form = listOf("W", "D", "W", "W", "L")),
        CastingStanding(rank = 4, teamName = "Chelsea", teamLogo = "chelsea_logo", played = 16, wins = 8, draws = 4, losses = 4, gd = 12, points = 28, form = listOf("W", "D", "W", "L", "W")),
        CastingStanding(rank = 5, teamName = "Crystal Palace", teamLogo = "palace_logo", played = 16, wins = 7, draws = 5, losses = 4, gd = 5, points = 26, form = listOf("D", "W", "L", "W", "D")),
        CastingStanding(rank = 6, teamName = "Man Utd", teamLogo = "manutd_logo", played = 15, wins = 7, draws = 4, losses = 4, gd = 4, points = 25, form = listOf("W", "L", "D", "W", "W"))
    )

    val mockLineup = CastingTeamLineup(
        teamName = "Barcelona",
        formation = "4-3-3",
        players = listOf(
            // GK
            CastingLineupPlayer(31, "Ter Stegen", 0.5f, 0.90f),
            // DF
            CastingLineupPlayer(2, "Koundé", 0.15f, 0.72f),
            CastingLineupPlayer(15, "Araújo", 0.38f, 0.75f),
            CastingLineupPlayer(6, "Christensen", 0.62f, 0.75f),
            CastingLineupPlayer(13, "Alba", 0.85f, 0.72f),
            // MF
            CastingLineupPlayer(25, "Busquets", 0.25f, 0.55f),
            CastingLineupPlayer(37, "De Jong", 0.50f, 0.58f),
            CastingLineupPlayer(8, "Pedri", 0.75f, 0.55f),
            // FW
            CastingLineupPlayer(7, "Dembélé", 0.18f, 0.30f),
            CastingLineupPlayer(30, "Lewandowski", 0.50f, 0.28f),
            CastingLineupPlayer(10, "Ferran", 0.82f, 0.30f)
        )
    )

    val mockLiveMatches = listOf(
        CastingLiveMatch(homeTeam = "Barcelona", homeScore = 3, awayTeam = "Real Madrid", awayScore = 2, competition = "Champions League", status = "LIVE • 87'", isLive = true),
        CastingLiveMatch(homeTeam = "Manchester City", homeScore = 1, awayTeam = "Liverpool", awayScore = 1, competition = "Premier League", status = "LIVE • 72'", isLive = true),
        CastingLiveMatch(homeTeam = "Bayern Munich", homeScore = 2, awayTeam = "PSG", awayScore = 0, competition = "Champions League", status = "Fulltid", isLive = false)
    )
}
