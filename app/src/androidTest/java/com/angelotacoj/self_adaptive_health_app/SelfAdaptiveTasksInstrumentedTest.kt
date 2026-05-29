package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptiveTiming
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase C1.7B: Instrumented tests for T2–T5 tasks.
 *
 * These tests exercise the current fixed-order STATIC condition UI.
 * All tests start a fresh session with code "0001" and unlock tasks sequentially.
 *
 * Key facts about the current UI:
 * - No Group A/B selector – all participants follow STATIC_UI → SELF_ADAPTIVE_UI.
 * - T3 (WellBeing): 4-step flow; Form step has energy level text field, mood selector,
 *   and optional note.
 * - T4 (Reminder): step-by-step activity/time/frequency selection.
 * - T5 (Summary): 4-step flow; ReinforcedConfirmation is a full screen step (not a
 *   modal dialog) with "Confirmar", "Editar", "Cancelar" buttons.
 * - In STATIC mode, no adaptive confirmation dialog ("Confirmar y continuar") appears.
 */
@RunWith(AndroidJUnit4::class)
class SelfAdaptiveTasksInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppData() {
        composeRule.resetResearchData()
        AdaptiveTiming.prolongedTimeDetectionEnabled = false
    }

    @After
    fun leaveTaskBeforeActivityDestroy() {
        AdaptiveTiming.prolongedTimeDetectionEnabled = true
        composeRule.clearActiveSessionBeforeActivityDestroy()
    }

    // ── T3 – WellBeing ────────────────────────────────────────────────────────

    /**
     * T3 Form step: pressing "Revisar registro" without selecting a mood shows
     * the field error message.
     * Error message (WellBeingViewModel L82): "Por favor seleccione un estado de ánimo simulado."
     * Also, empty energy triggers: "Ingrese un número entre 1 y 10 para el nivel de energía simulado."
     */
    @Test
    fun t3_fieldError_inStaticCondition_showsValidationMessage() {
        composeRule.startFixedOrderSession("T3A1")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 3:")
        composeRule.onNodeWithText("Iniciar formulario").performScrollTo().performClick()

        // Wait for form step
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Revisar registro").fetchSemanticsNodes().isNotEmpty()
        }

        // Try to advance without energy value or mood selection
        composeRule.onNodeWithText("Revisar registro").performScrollTo().performClick()

        // Error message should appear (energy error fires first)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Ingrese un número entre 1 y 10 para el nivel de energía simulado.").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Por favor seleccione un estado de ánimo simulado.").fetchSemanticsNodes().isNotEmpty()
        }
        val hasEnergyError = composeRule.onAllNodesWithText("Ingrese un número entre 1 y 10 para el nivel de energía simulado.").fetchSemanticsNodes().isNotEmpty()
        val hasMoodError = composeRule.onAllNodesWithText("Por favor seleccione un estado de ánimo simulado.").fetchSemanticsNodes().isNotEmpty()
        assert(hasEnergyError || hasMoodError) { "Expected WellBeing field error to appear" }
    }

    /**
     * T3 Review step: after entering a valid value and selecting a mood,
     * then pressing "Revisar registro", the review screen shows "Guardar registro".
     */
    @Test
    fun t3_validEntry_inStaticCondition_showsReviewStep() {
        composeRule.startFixedOrderSession("T3A2")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 3:")
        composeRule.onNodeWithText("Iniciar formulario").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Revisar registro").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction() and androidx.compose.ui.test.hasText("Nivel de energía simulado", substring = true)).performTextReplacement("7")
        composeRule.onNodeWithText("Neutral").performScrollTo().performClick()
        composeRule.onNodeWithText("Revisar registro").performScrollTo().performClick()

        // After validation, "Guardar registro" should appear on the Review step
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Guardar registro").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Guardar registro").performScrollTo().assertIsDisplayed()
    }

    // ── T4 – Reminder ─────────────────────────────────────────────────────────

    /**
     * T4: Pressing back during the reminder configuration flow triggers the
     * AR-04 navigation-hint suggestion (in ADAPTIVE mode) or simply returns
     * to the previous step (in STATIC mode). In STATIC mode we just verify
     * that back navigation works without crashing.
     */
    @Test
    fun t4_backNavigation_inStaticCondition_doesNotCrash() {
        composeRule.startFixedOrderSession("T4A1")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()
        composeRule.completeT3WellBeingForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 4:")
        composeRule.onNodeWithText("Iniciar configuración").performScrollTo().performClick()
        // SelectType step: select an activity and advance
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Vitamina ficticia").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Vitamina ficticia").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Continuar").apply {
            fetchSemanticsNodes().firstOrNull()?.let { get(0).performScrollTo().performClick() }
        }

        // Back navigation
        pressBack()
        composeRule.waitForIdle()
        pressBack()
        composeRule.waitForIdle()

        // Should still be inside the reminder screen or have returned home safely
        // (no crash is the acceptance criterion here)
        val onReminderScreen = composeRule.onAllNodesWithText("Recordatorio", substring = true).fetchSemanticsNodes().isNotEmpty()
        val onHomeScreen = composeRule.onAllNodesWithText("Inicio").fetchSemanticsNodes().isNotEmpty()
        assert(onReminderScreen || onHomeScreen) { "Expected to remain on Reminder screen or return home after back press" }
    }

    /**
     * T4: Completing all reminder selections and pressing "Guardar simulación"
     * advances to the final task step (Static: direct success or home).
     */
    @Test
    fun t4_completeReminder_inStaticCondition_showsCompletionOrHome() {
        composeRule.startFixedOrderSession("T4A2")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()
        composeRule.completeT3WellBeingForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 4:")
        composeRule.onNodeWithText("Iniciar configuración").performScrollTo().performClick()
        // SelectType
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Vitamina ficticia").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Vitamina ficticia").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Continuar").apply {
            fetchSemanticsNodes().firstOrNull()?.let { get(0).performScrollTo().performClick() }
        }
        // SelectSchedule: pick frequency; date/time come from DataSet
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Diariamente").fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithText("Seleccione una fecha", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        // Select date
        composeRule.onNode(androidx.compose.ui.test.hasText("Seleccione una fecha", substring = true, ignoreCase = true) and androidx.compose.ui.test.hasClickAction()).performScrollTo().performClick()
        composeRule.waitUntil(5000) { composeRule.onAllNodesWithText("Aceptar").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Aceptar").performClick()
        
        // Select time
        composeRule.onNode(androidx.compose.ui.test.hasText("Seleccione una hora", substring = true, ignoreCase = true) and androidx.compose.ui.test.hasClickAction()).performScrollTo().performClick()
        composeRule.waitUntil(5000) { composeRule.onAllNodesWithText("Aceptar").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Aceptar").performClick()
        
        // Select frequency
        composeRule.onNodeWithText("Diariamente").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Continuar").apply {
            fetchSemanticsNodes().firstOrNull()?.let { get(0).performScrollTo().performClick() }
        }
        // SelectDetails: skip optional fields
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Revisar configuración").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Revisar configuración").performScrollTo().performClick()
        // ReviewSummary: save
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Guardar simulación").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Guardar simulación").performScrollTo().performClick()

        // Static mode: should reach "Volver al inicio" on the completion step
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Volver al inicio").assertIsDisplayed()
    }

    // ── T5 – Summary (reinforced confirmation screen) ─────────────────────────

    /**
     * T5 ReinforcedConfirmation step: After "Guardar información", the full
     * confirmation step is displayed. Verify that "Confirmar" and "Editar"
     * buttons are visible (the step is scrollable and buttons are reachable).
     *
     * This is the regression test for the blocking T5 modal bug (Phase C1.6F):
     * The confirmation was moved from an un-scrollable AlertDialog into a
     * scrollable ScreenContainer step. Both action buttons must be reachable.
     */
    @Test
    fun t5_reinforcedConfirmation_buttonsAreReachable() {
        composeRule.startFixedOrderSession("T5A1")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()
        composeRule.completeT3WellBeingForSequentialAccess()
        composeRule.completeT4ReminderForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 5:")
        composeRule.onNodeWithText("Revisar detalles").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar información").performScrollTo().performClick()

        // ReinforcedConfirmation step is a full scrollable screen, not a dialog
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Confirmar").fetchSemanticsNodes().isNotEmpty()
        }

        // Both primary and secondary action buttons must be visible/reachable
        composeRule.onNodeWithText("Confirmar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Editar").performScrollTo().assertIsDisplayed()
        // "Cancelar" is shown when safeExitEnabled OR in static mode
        // In static mode it should be visible too
        composeRule.onNodeWithText("Cancelar").performScrollTo().assertIsDisplayed()
    }

    /**
     * T5: Pressing "Confirmar" on the ReinforcedConfirmation step advances to
     * the Final step and then shows "Volver al inicio".
     */
    @Test
    fun t5_confirmButton_inStaticCondition_completesTask() {
        composeRule.startFixedOrderSession("T5A2")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()
        composeRule.completeT3WellBeingForSequentialAccess()
        composeRule.completeT4ReminderForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 5:")
        composeRule.onNodeWithText("Revisar detalles").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar información").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Confirmar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Confirmar").performScrollTo().performClick()

        // Final step auto-navigates to UEQ directly in the real app flow
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Siguiente").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Siguiente").assertIsDisplayed()
    }

    /**
     * T5: Pressing "Editar" on the ReinforcedConfirmation step returns to the
     * Details step (Step 2 of 4) so the user can review again.
     */
    @Test
    fun t5_editButton_onReinforcedConfirmation_returnsToDetails() {
        composeRule.startFixedOrderSession("T5A3")
        composeRule.completeT1AccessToUnlockTasks()
        composeRule.completeT2AppointmentForSequentialAccess()
        composeRule.completeT3WellBeingForSequentialAccess()
        composeRule.completeT4ReminderForSequentialAccess()

        composeRule.openTaskByTitle("Tarea 5:")
        composeRule.onNodeWithText("Revisar detalles").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar información").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Confirmar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Editar").performScrollTo().performClick()

        // Should return to Details step or Intro step
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Guardar información").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Revisar detalles").fetchSemanticsNodes().isNotEmpty()
        }
        val returnedToReview =
            composeRule.onAllNodesWithText("Guardar información").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Revisar detalles").fetchSemanticsNodes().isNotEmpty()
        assert(returnedToReview) { "Expected to return to Details or Intro step after pressing Editar" }
    }
}
