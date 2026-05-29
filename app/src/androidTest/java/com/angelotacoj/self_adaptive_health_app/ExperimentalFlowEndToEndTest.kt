package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptiveTiming
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase C1.7B: End-to-end instrumented tests for the fixed experiment flow.
 *
 * Final methodology:
 *   Setup → Initial Profile → STATIC T1–T5 → UEQ_STATIC (26 items)
 *       → ConditionTransition → ADAPTIVE T1–T5 → UEQ_ADAPTIVE (26 items)
 *       → Interview → SessionCompleted
 *
 * No Group A/B. No UEQ-S. Participant code is a stable 4-char alphanumeric string.
 * Room database version = 1. App data cleared before each test via resetResearchData().
 *
 * Test coverage:
 * E2E-01: Static condition completes T1–T5 and routes to UEQ.
 * E2E-02: After Static UEQ, condition transition screen is shown.
 * E2E-03: Completed T1 shows as disabled on Home screen (Room-backed).
 * E2E-04: Static condition does not show adaptive affordances ("Necesito ayuda").
 * E2E-05: Full session flow Static → UEQ → Transition → Adaptive → UEQ → Interview → SessionCompleted.
 * E2E-06: Interview skip routes to SessionCompleted.
 */
@RunWith(AndroidJUnit4::class)
class ExperimentalFlowEndToEndTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppData() {
        composeRule.resetResearchData()
        AdaptiveTiming.prolongedTimeDetectionEnabled = false
    }

    @After
    fun restoreAdaptiveTiming() {
        AdaptiveTiming.prolongedTimeDetectionEnabled = true
        composeRule.clearActiveSessionBeforeActivityDestroy()
    }

    // ── E2E-01: Static T1–T5 completion routes to UEQ ────────────────────────

    /**
     * After completing all 5 static tasks, the home screen shows
     * "Continuar con el UEQ" and the UEQ screen is reachable.
     */
    @Test
    fun e2e01_staticConditionComplete_routesToUeqScreen() {
        composeRule.startFixedOrderSession("E001")
        composeRule.completeStaticConditionTasks()

        // After T5 completes, app auto-navigates to UEQ
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("UEQ").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Cuestionario completado").fetchSemanticsNodes().isNotEmpty()
        }
        val ueqVisible = composeRule.onAllNodesWithText("UEQ").fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithText("Cuestionario completado").fetchSemanticsNodes().isNotEmpty()
        assert(ueqVisible) { "Expected UEQ screen after completing Static condition tasks" }
    }

    // ── E2E-02: Static UEQ → ConditionTransition ─────────────────────────────

    /**
     * After completing Static tasks and the UEQ, the ConditionTransition screen
     * ("Primera etapa completada") is shown with a continue button.
     */
    @Test
    fun e2e02_staticUeq_routesToConditionTransition() {
        composeRule.startFixedOrderSession("E002")
        composeRule.completeStaticConditionTasks()
        composeRule.completeOfficialUeq26()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Primera etapa completada").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Primera etapa completada").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar con la segunda interfaz").performScrollTo().assertIsDisplayed()
    }

    // ── E2E-03: Room-backed task completion persists on Home ──────────────────

    /**
     * After completing T1, returning to Home shows T1 button as disabled (completed)
     * and the completion is persisted in Room (backed by DAO).
     */
    @Test
    fun e2e03_completedT1_persistsInRoomAndDisablesButton() {
        composeRule.startFixedOrderSession("E003")
        composeRule.completeT1AccessToUnlockTasks()

        // T1 button should be disabled (completed)
        composeRule.onNodeWithTag("start_t1_access").performScrollTo().assertIsNotEnabled()

        // Verify Room has exactly 1 completed task in STATIC condition
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val dao = AppContainer.database.experimentDao()
                val sessionId = AppContainer.experimentPreferences.sessionSnapshot.first().currentSessionId
                    ?: return@runBlocking false
                dao.getCompletedTaskCount(sessionId, "STATIC_UI") == 1
            }
        }
        val staticCount = runBlocking {
            val dao = AppContainer.database.experimentDao()
            val sessionId = AppContainer.experimentPreferences.sessionSnapshot.first().currentSessionId!!
            dao.getCompletedTaskCount(sessionId, "STATIC_UI")
        }
        assert(staticCount == 1) { "Expected 1 completed task in STATIC_UI, got $staticCount" }
    }

    // ── E2E-04: Static condition has no adaptive affordances ──────────────────

    /**
     * In the STATIC condition:
     * - "Ayuda: explicar esta sesión" button is NOT visible on Home.
     * - T3 form does not show "Necesito ayuda" or adaptation banners.
     */
    @Test
    fun e2e04_staticCondition_doesNotShowAdaptiveAffordances() {
        composeRule.startFixedOrderSession("E004")

        // "Ayuda: explicar esta sesión" only shows in ADAPTIVE mode
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Ayuda: explicar esta sesión").fetchSemanticsNodes().isEmpty()
        }
        assert(
            composeRule.onAllNodesWithText("Ayuda: explicar esta sesión").fetchSemanticsNodes().isEmpty()
        ) { "Expected 'Ayuda: explicar esta sesión' to be absent in STATIC condition" }

        // Open T1 and check for absence of adaptive help
        composeRule.openTaskByTitle("Tarea 1:")
        composeRule.onNodeWithText("Comenzar").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Código de usuario").fetchSemanticsNodes().isNotEmpty()
        }
        assert(
            composeRule.onAllNodesWithText("Necesito ayuda").fetchSemanticsNodes().isEmpty()
        ) { "Expected 'Necesito ayuda' to be absent in STATIC condition" }
        assert(
            composeRule.onAllNodesWithText("Sugerencia de adaptación").fetchSemanticsNodes().isEmpty()
        ) { "Expected no adaptive suggestion cards in STATIC condition" }

        composeRule.pressBackBestEffort()
    }

    // ── E2E-05: Full session flow ─────────────────────────────────────────────

    /**
     * Full end-to-end session:
     *   Static T1–T5 → UEQ (26) → ConditionTransition → Adaptive T1–T5
     *   → UEQ (26) → Interview (skip) → SessionCompleted
     *
     * This test takes several minutes on a real device. It is the primary
     * acceptance test for the Phase C1.7B milestone.
     */
    @Test
    fun e2e05_fullSessionFlow_staticToAdaptiveToInterview_reachesSessionCompleted() {
        composeRule.startFixedOrderSession("E005")

        // --- STATIC condition ---
        composeRule.completeStaticConditionTasks()

        // --- Static UEQ ---
        composeRule.completeOfficialUeq26()

        // --- Condition Transition ---
        composeRule.continueToAdaptiveCondition()

        // Verify we are now in ADAPTIVE (Etapa 2)
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Etapa 2").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Etapa 2").assertExists()

        // --- ADAPTIVE condition ---
        composeRule.completeAdaptiveConditionTasks()

        // --- Adaptive UEQ ---
        composeRule.completeOfficialUeq26()

        // --- Interview (skip with confirmation) ---
        composeRule.completeInterviewBySkippingWithConfirmation()

        // --- SessionCompleted ---
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Sesión experimental completada").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("completada").fetchSemanticsNodes().isNotEmpty()
        }
        val sessionComplete =
            composeRule.onAllNodesWithText("Sesión experimental completada").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Gracias").fetchSemanticsNodes().isNotEmpty()
        assert(sessionComplete) { "Expected SessionCompleted screen after full session flow" }
    }

    // ── E2E-06: Interview skip routes to SessionCompleted ────────────────────

    /**
     * After completing both conditions and both UEQs, skipping the interview
     * with confirmation navigates to the SessionCompleted screen.
     * Room should have 10 completed tasks (5 STATIC + 5 ADAPTIVE).
     */
    @Test
    fun e2e06_interviewSkip_routesToSessionCompleted_withTenCompletedTasks() {
        composeRule.startFixedOrderSession("E006")
        composeRule.completeStaticConditionTasks()
        composeRule.completeOfficialUeq26()
        composeRule.continueToAdaptiveCondition()
        composeRule.completeAdaptiveConditionTasks()
        composeRule.completeOfficialUeq26()
        composeRule.completeInterviewBySkippingWithConfirmation()

        // Wait for SessionCompleted screen
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Sesión experimental completada").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Gracias").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify 10 total completed tasks via Room
        // The session ID is retrievable from the DataStore snapshot stored before session ended
        val totalCompleted = runBlocking {
            val dao = AppContainer.database.experimentDao()
            // Try active session first; fall back to any recently completed session
            val sessionId = AppContainer.experimentPreferences.sessionSnapshot.first().currentSessionId
            val targetSessionId = sessionId ?: dao.getSessionByParticipantCode("P01-E006").firstOrNull()?.sessionId
            if (targetSessionId != null) {
                dao.getCompletedTaskCount(targetSessionId, "STATIC_UI") + 
                dao.getCompletedTaskCount(targetSessionId, "SELF_ADAPTIVE_UI")
            } else {
                -1
            }
        }
        assert(totalCompleted == 10) {
            "Expected 10 total completed tasks (5 STATIC + 5 ADAPTIVE), got $totalCompleted"
        }
    }
}

