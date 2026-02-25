package io.reachu.VioCastingUI.models

import java.util.UUID

data class MatchStatistics(
    val id: String = UUID.randomUUID().toString(),
    val stats: List<StatItem>
) {
    companion object {
        fun mock(forMatch: Match): MatchStatistics {
            return MatchStatistics(
                stats = listOf(
                    StatItem(name = "Possession", homeValue = 45.0, awayValue = 55.0, unit = "%"),
                    StatItem(name = "Shots", homeValue = 12.0, awayValue = 15.0),
                    StatItem(name = "Shots on Target", homeValue = 5.0, awayValue = 7.0),
                    StatItem(name = "Corners", homeValue = 4.0, awayValue = 6.0),
                    StatItem(name = "Fouls", homeValue = 10.0, awayValue = 8.0)
                )
            )
        }
    }
}

data class StatItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val homeValue: Double,
    val awayValue: Double,
    val unit: String = ""
)
