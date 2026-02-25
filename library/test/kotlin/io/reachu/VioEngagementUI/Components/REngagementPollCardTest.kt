package io.reachu.VioEngagementUI.Components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class VioEngagementPollCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pollCard_muestraPreguntaYOpciones_yDisparaOnVote() {
        val poll = Poll(
            id = "poll-1",
            broadcastId = "b1",
            question = "¿Cuál es tu color favorito?",
            options = listOf(
                PollOption(id = "opt-1", text = "Rojo"),
                PollOption(id = "opt-2", text = "Azul"),
            ),
        )

        var votedOptionId: String? = null

        composeRule.setContent {
            MaterialTheme {
                VioEngagementPollCard(
                    poll = poll,
                    pollResults = null,
                    sponsorLogoUrl = null,
                    onVote = { votedOptionId = it },
                    onDismiss = {},
                )
            }
        }

        // Verificamos la pregunta y una opción
        composeRule.onNodeWithText("¿Cuál es tu color favorito?").assertIsDisplayed()
        composeRule.onNodeWithText("Rojo").assertIsDisplayed()

        // Hacemos click en la opción y verificamos callback
        composeRule.onNodeWithText("Rojo").performClick()

        assert(votedOptionId == "opt-1")
    }
}

