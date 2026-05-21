package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
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
            contextualHelpVisible = true,
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
            contextualHelpVisible = false,
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
            ruleId = AdaptationRuleId.AR02,
            title = "Suggestion Title",
            message = "Suggestion Message",
            modifications = listOf(UiModification.UIM01_TEXT_SIZE),
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
}
