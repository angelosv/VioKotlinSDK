package com.reachu.tv2demo.ui.model

import io.reachu.VioCore.models.MatchContext
import java.util.UUID

/**
 * Convierte el modelo Match del demo en un MatchContext para usar con el SDK.
 * Genera un matchId único y estable desde datos del match.
 */
fun Match.toMatchContext(channelId: Int? = null): MatchContext {
    // Generar matchId único desde datos del match
    val matchId = generateMatchId()
    return MatchContext(
        matchId = matchId,
        matchName = title,
        startTime = null,  // No disponible en el modelo Match actual
        homeTeamId = this.homeTeam.id,
        awayTeamId = this.awayTeam.id,
        competitionId = this.competition,
        channelId = channelId,
        metadata = mapOf(
            "competition" to competition,
            "venue" to venue,
            "homeTeam" to homeTeam.name,
            "awayTeam" to awayTeam.name,
            "isLive" to isLive.toString(),
        )
    )
}

/**
 * Genera un ID estable desde datos del match.
 * Normaliza nombres de equipos a slugs (lowercase, sin espacios).
 * Permite casos especiales con IDs hardcodeados si es necesario.
 */
private fun Match.generateMatchId(): String {
    // Para casos especiales (ej: Barcelona-PSG), usar ID específico
    if (title.contains("Barcelona", ignoreCase = true) && title.contains("PSG", ignoreCase = true)) {
        return "barcelona-psg-2025-01-23"
    }
    
    // Crear ID estable desde datos del match
    val homeTeamSlug = homeTeam.name.lowercase()
        .replace(" ", "-")
        .replace("fc", "")
        .replace(".", "")
        .trim()
        .filter { it.isLetterOrDigit() || it == '-' }
    val awayTeamSlug = awayTeam.name.lowercase()
        .replace(" ", "-")
        .replace("fc", "")
        .replace(".", "")
        .trim()
        .filter { it.isLetterOrDigit() || it == '-' }
    val competitionSlug = competition.lowercase()
        .replace(" ", "-")
        .filter { it.isLetterOrDigit() || it == '-' }
    
    return "$homeTeamSlug-$awayTeamSlug-$competitionSlug"
}

data class Team(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortName: String,
    val logo: String,
)

sealed class MatchAvailability {
    object Available : MatchAvailability()
    data class AvailableUntil(val date: String) : MatchAvailability()
    data class Upcoming(val date: String) : MatchAvailability()

    val title: String
        get() = when (this) {
            is Available, is AvailableUntil -> "Tilgjengelighet"
            is Upcoming -> "Kommer snart"
        }

    val description: String
        get() = when (this) {
            is Available -> "Tilgjengelig nå"
            is AvailableUntil -> "Tilgjengelig lenger enn $date"
            is Upcoming -> date
        }
}

data class RelatedTeam(
    val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val description: String? = null,
)

data class Match(
    val id: String = UUID.randomUUID().toString(),
    val homeTeam: Team,
    val awayTeam: Team,
    val title: String,
    val subtitle: String,
    val competition: String,
    val venue: String,
    val commentator: String? = null,
    val isLive: Boolean = false,
    val backgroundImage: String,
    val availability: MatchAvailability,
    val relatedContent: List<RelatedTeam> = emptyList(),
    val campaignLogo: String? = null,
)

object MatchMocks {
    private val barcelona = Team(
        name = "FC Barcelona",
        shortName = "Barcelona",
        logo = "barcelona_logo",
    )
    private val psg = Team(
        name = "Paris Saint-Germain",
        shortName = "PSG",
        logo = "psg_logo",
    )

    val barcelonaVsPsg = Match(
        homeTeam = barcelona,
        awayTeam = psg,
        title = "Barcelona - PSG",
        subtitle = "UEFA Champions League • Fotball",
        competition = "UEFA Champions League",
        venue = "Camp Nou",
        commentator = "Magnus Drivenes",
        isLive = false,
        backgroundImage = "https://www.ole.com.ar/images/2025/09/30/kV1XGizgI_1200x630__1.jpg",
        availability = MatchAvailability.Upcoming("Kommer 12. november"),
        relatedContent = listOf(
            RelatedTeam(team = barcelona),
            RelatedTeam(team = psg),
        ),
        campaignLogo = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Adidas_logo.png/800px-Adidas_logo.png",
    )

    val dortmundVsAthletic = Match(
        homeTeam = Team(name = "Borussia Dortmund", shortName = "Dortmund", logo = "bvb_logo"),
        awayTeam = Team(name = "Athletic Club", shortName = "Athletic", logo = "athletic_logo"),
        title = "Dortmund - Athletic",
        subtitle = "UEFA Champions League • Fotball",
        competition = "UEFA Champions League",
        venue = "SIGNAL IDUNA PARK",
        commentator = "Magnus Drivenes",
        isLive = false,
        backgroundImage = "https://1529406180.rsc.cdn77.org/df/deportes/futbol/champions/es/786633_0.jpg",
        availability = MatchAvailability.AvailableUntil("ett år"),
        relatedContent = listOf(
            RelatedTeam(team = Team(name = "Borussia Dortmund", shortName = "BVB", logo = "bvb_logo")),
            RelatedTeam(team = Team(name = "Athletic Club", shortName = "Athletic", logo = "athletic_logo")),
        ),
    )

    val cityVsReal = Match(
        homeTeam = Team(name = "Manchester City", shortName = "City", logo = "city_logo"),
        awayTeam = Team(name = "Real Madrid", shortName = "Madrid", logo = "madrid_logo"),
        title = "Man City - Real Madrid",
        subtitle = "UEFA Champions League • Fotball",
        competition = "UEFA Champions League",
        venue = "Etihad Stadium",
        commentator = "Øyvind Alsaker",
        isLive = true,
        backgroundImage = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQM6atVnC3T4SNnl0vkyj6Q-_kTQSiNFgV68Q&s",
        availability = MatchAvailability.Available,
    )

    val mockMatches = listOf(barcelonaVsPsg, dortmundVsAthletic, cityVsReal)
}
