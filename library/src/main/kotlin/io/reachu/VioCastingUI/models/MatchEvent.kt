package io.reachu.VioCastingUI.models

import java.util.UUID

data class MatchEvent(
    val id: String = UUID.randomUUID().toString(),
    val minute: Int,
    val type: MatchEventType,
    val player: String? = null,
    val team: TeamType = TeamType.HOME,
    val description: String? = null,
    val score: String? = null
)

sealed class MatchEventType {
    object Goal : MatchEventType()
    data class Substitution(val playerOn: String, val playerOff: String) : MatchEventType()
    object YellowCard : MatchEventType()
    object RedCard : MatchEventType()
    object KickOff : MatchEventType()
    object HalfTime : MatchEventType()
    object FullTime : MatchEventType()
    object Other : MatchEventType()
}

enum class TeamType {
    HOME, AWAY
}
