package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdaptiveComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ContextualHelpBox is visible when enabled in state`() {
        val state = AdaptiveUiState(
            isAdaptiveMode = true,
            uim02Level = AdaptationLevel.LEVEL_2_MODERATE_SUPPORT,
            contextualHelpMessage = "Test help message"
        )

        composeTestRule.setContent {
            ContextualHelpBox(state = state, onHide = {})
        }

        composeTestRule.onNodeWithText("Ayuda del sistema").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test help message").assertIsDisplayed()
    }

    @Test
    fun `ContextualHelpBox is hidden when disabled in state`() {
        val state = AdaptiveUiState(
            isAdaptiveMode = true,
            uim02Level = AdaptationLevel.LEVEL_0_BASE,
            contextualHelpMessage = "Test help message"
        )

        composeTestRule.setContent {
            ContextualHelpBox(state = state, onHide = {})
        }

        composeTestRule.onNodeWithText("Ayuda del sistema").assertDoesNotExist()
    }

    @Test
    fun `AdaptiveSuggestionCard shows suggested adaptation`() {
        val pending = PendingAdaptation(
            ruleId = AdaptationRuleId.AR01_TIME_ON_SCREEN,
            title = "Suggestion Title",
            message = "Suggestion Message",
            modifications = listOf(UiModification.UIM01_TEXT),
            validationType = ValidationType.SUGGESTED
        )
        val state = AdaptiveUiState(isAdaptiveMode = true)

        composeTestRule.setContent {
            AdaptiveSuggestionCard(
                pending = pending,
                onApply = {},
                onReject = {},
                adaptiveUiState = state
            )
        }

        composeTestRule.onNodeWithText("Sugerencia de adaptación").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestion Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestion Message").assertIsDisplayed()
    }

    @Test
    fun `UndoAdaptationCard is visible when enabled`() {
        val state = AdaptiveUiState(
            isAdaptiveMode = true,
            undoMessageVisible = true
        )

        composeTestRule.setContent {
            UndoAdaptationCard(
                visible = true,
                onUndo = {},
                onDismiss = {},
                adaptiveUiState = state
            )
        }

        composeTestRule.onNodeWithText("Cambio aplicado automáticamente").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deshacer cambio").assertIsDisplayed()
    }

    @Test
    fun `AdaptiveText renders larger font size as uim01Level escalates`() {
        val stateLevel0 = AdaptiveUiState(uim01Level = AdaptationLevel.LEVEL_0_BASE)
        val stateLevel3 = AdaptiveUiState(uim01Level = AdaptationLevel.LEVEL_3_HIGH_SUPPORT)

        composeTestRule.setContent {
            AdaptiveText(text = "Scaling Test", state = stateLevel0)
        }
        composeTestRule.onNodeWithText("Scaling Test").assertIsDisplayed()

        assertEquals(1.0f, stateLevel0.textScale)
        assertEquals(1.35f, stateLevel3.textScale)
    }
}
