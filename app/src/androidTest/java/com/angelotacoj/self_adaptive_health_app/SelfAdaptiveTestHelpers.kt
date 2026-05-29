package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptiveTiming
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import kotlinx.coroutines.runBlocking

typealias MainComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

// ---------------------------------------------------------------------------
//  App state reset
// ---------------------------------------------------------------------------

fun MainComposeRule.resetResearchData() {
    AdaptiveTiming.prolongedTimeDetectionEnabled = true
    val context = activity.applicationContext
    AppContainer.init(context)
    runBlocking {
        AppContainer.database.experimentDao().clearAll()
        AppContainer.experimentPreferences.clearActiveSessionPreferences()
    }
    AppContainer.knowledgeRepository.clearCurrentTaskAdaptationMemory()
    AppContainer.experimentLogger.clear()
    // Wait for setup screen – no group selector exists in current UI
    waitUntil(timeoutMillis = 8_000) {
        onAllNodesWithText("Configuración inicial AURA").fetchSemanticsNodes().isNotEmpty() ||
            runCatching { onNodeWithTag("participant_suffix_input").fetchSemanticsNode() }.isSuccess
    }
}

fun MainComposeRule.clearActiveSessionBeforeActivityDestroy() {
    runCatching {
        runBlocking {
            AppContainer.experimentPreferences.clearActiveSessionPreferences()
            AppContainer.database.experimentDao().clearAll()
        }
    }
}

// ---------------------------------------------------------------------------
//  Session start – no Group A/B; fixed order always STATIC → ADAPTIVE
// ---------------------------------------------------------------------------

/**
 * Start a fixed-order session with [participantCode] (exactly 4 alphanumeric chars, e.g. "0001").
 * Returns the participant code that was entered.
 *
 * Note: Group A/B selection no longer exists in the UI. All participants follow
 * STATIC_UI → SELF_ADAPTIVE_UI automatically.
 */
fun MainComposeRule.startFixedOrderSession(participantCode: String = "0001"): String {
    val code = participantCode.uppercase().filter { it.isLetterOrDigit() }.take(4)
    onNodeWithTag("participant_suffix_input").performTextClearance()
    onNodeWithTag("participant_suffix_input").performTextReplacement(code)
    onNodeWithTag("continue_button").performScrollTo().performClick()
    completeInitialProfileIfPresent()
    waitForHome()
    return code
}

/**
 * Deprecated aliases kept for compile compatibility – both map to [startFixedOrderSession].
 * Group A/B selection was removed in Phase C1.5.
 */
fun MainComposeRule.startGroupBSession(participantId: String): String =
    startFixedOrderSession(participantId)

fun MainComposeRule.startGroupASession(participantId: String): String =
    startFixedOrderSession(participantId)

// ---------------------------------------------------------------------------
//  Initial profile helper
// ---------------------------------------------------------------------------

fun MainComposeRule.completeInitialProfileIfPresent() {
    waitForIdle()
    val profileVisible = runCatching {
        waitUntil(timeoutMillis = 8_000) {
            onAllNodesWithText("Construcción del perfil inicial").fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText("Inicio").fetchSemanticsNodes().isNotEmpty()
        }
    }.isSuccess

    if (!profileVisible) return

    // If Home is already visible, profile was skipped or already completed.
    if (onAllNodesWithText("Inicio").fetchSemanticsNodes().isNotEmpty()) return

    listOf(
        "profile_question_1_yes",
        "profile_question_2_yes",
        "profile_question_3_yes",
        "profile_question_4_only_if_needed",
        "profile_question_5_important_only",
        "profile_question_6_regular",
        "profile_question_7_yes",
        "profile_question_8_important_only"
    ).forEach { tag ->
        onNodeWithTag(tag).performScrollTo().performClick()
    }
    onNodeWithTag("profile_continue_button").performScrollTo().performClick()
}

// ---------------------------------------------------------------------------
//  Home navigation helpers
// ---------------------------------------------------------------------------

fun MainComposeRule.waitForHome() {
    waitUntil(timeoutMillis = 8_000) {
        onAllNodesWithText("Tareas de salud simulada", substring = true).fetchSemanticsNodes().isNotEmpty()
    }
}

fun MainComposeRule.openTaskByTitle(title: String) {
    onNodeWithText(title, substring = true).performScrollTo()
    val tag = when {
        title.contains("T1") || title.contains("Tarea 1") -> "start_t1_access"
        title.contains("T2") || title.contains("Tarea 2") -> "start_t2_appointment"
        title.contains("T3") || title.contains("Tarea 3") -> "start_t3_wellbeing"
        title.contains("T4") || title.contains("Tarea 4") -> "start_t4_reminder"
        title.contains("T5") || title.contains("Tarea 5") -> "start_t5_summary"
        else -> error("Unknown task title: $title")
    }
    onNodeWithTag(tag).performScrollTo().performClick()
}

// ---------------------------------------------------------------------------
//  T1 – Access (T1_ACCESS)
// ---------------------------------------------------------------------------

/**
 * Completes T1 Access task in STATIC condition (no adaptive confirmation dialog).
 *
 * Flow: Intro → Comenzar → Code step (enter userCode) → Continuar → PIN step (enter pin)
 *       → Validar acceso → Validation step → Finalizar tarea → Completed → Volver al inicio.
 * Note: field label is "Código de usuario" (not "Código de usuario simulado").
 *       PIN field label in UI is "PIN" but the OutlinedTextField label text is "PIN".
 */
fun MainComposeRule.completeT1AccessToUnlockTasks(
    userCode: String = "PACIENTE2391",
    pin: String = "928102"
) {
    openTaskByTitle("Tarea 1:")
    onNodeWithText("Comenzar").performScrollTo().performClick()
    // Code step
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Código de usuario").fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasSetTextAction()).performTextInput(userCode)
    onNodeWithText("Continuar").performScrollTo().performClick()
    // PIN step
    waitUntil(timeoutMillis = 8_000) {
        onAllNodesWithText("Validar acceso").fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasSetTextAction()).performTextInput(pin)
    onNodeWithText("Validar acceso").performScrollTo().performClick()
    // Validation step (AccessStep.Validation)
    onNodeWithText("Finalizar tarea").performScrollTo().performClick()
    // Completed step
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitForHome()
}

/**
 * Completes T1 in ADAPTIVE condition.
 * In ADAPTIVE mode, "Validar acceso" may trigger an AdaptiveConfirmationDialog
 * (AR-08). The confirmation is handled automatically if present.
 */
fun MainComposeRule.completeT1AccessAdaptive(
    userCode: String = "PACIENTE1830",
    pin: String = "830132"
) {
    openTaskByTitle("Tarea 1:")
    onNodeWithText("Comenzar").performScrollTo().performClick()
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Código de usuario").fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasSetTextAction()).performTextInput(userCode)
    onNodeWithText("Continuar").performScrollTo().performClick()
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Validar acceso").fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasSetTextAction()).performTextInput(pin)
    onNodeWithText("Validar acceso").performScrollTo().performClick()
    // In ADAPTIVE mode, AR-08 AdaptiveConfirmationDialog may show:
    waitUntil(timeoutMillis = 8_000) {
        onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty() ||
        onAllNodesWithText("Finalizar tarea").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Confirmar información simulada").performScrollTo().performClick()
    } else {
        onNodeWithText("Finalizar tarea").performScrollTo().performClick()
    }
    // Completed step
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitForHome()
}

// ---------------------------------------------------------------------------
//  T2 – Appointment (T2_APPOINTMENT)
// ---------------------------------------------------------------------------

/**
 * Completes T2 Appointment task.
 * Flow: Overview → Ver lista de citas → List step → click appointment card (Abrir {title})
 *       → Detail step → Confirmar → Confirmation step → Completar la tarea → Completed → Volver al inicio.
 */
fun MainComposeRule.completeT2AppointmentForSequentialAccess() {
    openTaskByTitle("Tarea 2:")
    onNodeWithText("Ver lista de citas").performScrollTo().performClick()
    // List step: click the first appointment's "Abrir..." button
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Abrir ", substring = true).fetchSemanticsNodes().isNotEmpty()
    }
    onAllNodesWithText("Abrir ", substring = true)[0].performScrollTo().performClick()
    // Detail step
    onNodeWithText("Confirmar").performScrollTo().performClick()
    // Confirmation step
    onNodeWithText("Completar la tarea").performScrollTo().performClick()
    // Completed step
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitForHome()
}

// ---------------------------------------------------------------------------
//  T3 – Well-being (T3_WELL_BEING)
// ---------------------------------------------------------------------------

/**
 * Completes T3 WellBeing task.
 * Flow: Intro → Iniciar formulario → Form step (enter energy=5, mood=Neutral)
 *       → Revisar registro → Review step → Guardar registro → Success → Volver al inicio.
 * ADAPTIVE mode may show AdaptiveConfirmationDialog → "Confirmar información simulada".
 */
fun MainComposeRule.completeT3WellBeingForSequentialAccess() {
    openTaskByTitle("Tarea 3:")
    onNodeWithText("Iniciar formulario").performScrollTo().performClick()
    // Form step: energy level + mood selector (both required)
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Revisar registro").fetchSemanticsNodes().isNotEmpty()
    }
    onNode(hasSetTextAction() and hasText("Nivel de energía simulado", substring = true)).performTextReplacement("5")
    onNodeWithText("Neutral").performScrollTo().performClick()
    onNodeWithText("Revisar registro").performScrollTo().performClick()
    // Review step
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Guardar registro").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Guardar registro").performScrollTo().performClick()

    // It may show the adaptive dialog or go straight to Success
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Confirmar información simulada").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
        }
    }
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitForHome()
}

// ---------------------------------------------------------------------------
//  T4 – Reminder (T4_REMINDER)
// ---------------------------------------------------------------------------

/**
 * Completes T4 Reminder task.
 * Flow: Intro → Iniciar configuración → SelectType step (Vitamina ficticia + Continuar)
 *       → SelectSchedule step (preselect frequency + Continuar, no date/time picker needed)
 *       → SelectDetails → Revisar configuración → ReviewSummary → Guardar simulación
 *       → Saved → Volver al inicio.
 *
 * Note: SelectSchedule requires date, time AND frequency all non-blank. Tests use
 * CheckableOptionRow for frequency but the date/time pickers open native dialogs which
 * are not easily automatable. We pick Diariamente as frequency and rely on
 * pre-populated data (state.selectedDate/Time are pre-set from DataSet). If the
 * "Continuar" button is disabled (date/time blank), a fallback clicks the default values.
 */
fun MainComposeRule.completeT4ReminderForSequentialAccess() {
    openTaskByTitle("Tarea 4:")
    onNodeWithText("Iniciar configuración").performScrollTo().performClick()
    // SelectType step – choose first option
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Vitamina ficticia").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Vitamina ficticia").performScrollTo().performClick()
    onAllNodesWithText("Continuar").apply {
        fetchSemanticsNodes().firstOrNull()?.let { get(0).performScrollTo().performClick() }
    }
    // SelectSchedule step – select a frequency; date/time may already be pre-populated
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Diariamente").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Continuar").fetchSemanticsNodes().isNotEmpty()
    }
    // Select date
    onNodeWithText("Seleccione una fecha", substring = true, ignoreCase = true).performScrollTo().performClick()
    waitUntil(5000) { onAllNodesWithText("Aceptar").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText("Aceptar").performClick()
    
    // Select time
    onNodeWithText("Seleccione una hora", substring = true, ignoreCase = true).performScrollTo().performClick()
    waitUntil(5000) { onAllNodesWithText("Aceptar").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText("Aceptar").performClick()
    
    // Select frequency
    onNodeWithText("Diariamente").performScrollTo().performClick()
    // Try to press Continuar (enabled if date+time+frequency are set)
    onAllNodesWithText("Continuar").apply {
        fetchSemanticsNodes().firstOrNull()?.let { get(0).performScrollTo().performClick() }
    }
    // SelectDetails step – skip optional fields
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Revisar configuración").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Revisar configuración").performScrollTo().performClick()
    // ReviewSummary step
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Guardar simulación").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Guardar simulación").performScrollTo().performClick()

    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Confirmar información simulada").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Volver al inicio").fetchSemanticsNodes().isNotEmpty()
        }
    }
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitForHome()
}

// ---------------------------------------------------------------------------
//  T5 – Summary / final review (T5_SUMMARY)
// ---------------------------------------------------------------------------

/**
 * Completes T5 Summary task.
 * Flow: Intro → Revisar detalles → Details step → Guardar información
 *       → ReinforcedConfirmation step (full scrollable screen, NOT a dialog)
 *       → Confirmar → Final step → Volver al inicio.
 *
 * In STATIC mode, the ReinforcedConfirmation step always shows "Confirmar" and "Cancelar".
 * In ADAPTIVE mode, an AdaptiveConfirmationDialog may intercept "Guardar información" first.
 */
fun MainComposeRule.completeT5Summary() {
    openTaskByTitle("Tarea 5:")
    onNodeWithText("Revisar detalles").performScrollTo().performClick()
    // Details step
    onNodeWithText("Guardar información").performScrollTo().performClick()
    waitUntil(timeoutMillis = 8_000) {
        onAllNodesWithText("Confirmar").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithText("Confirmar información simulada").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Confirmar información simulada").performScrollTo().performClick()
        // Wait for ReinforcedConfirmation step to appear after dialog
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Confirmar").fetchSemanticsNodes().isNotEmpty()
        }
    }
    onNodeWithText("Confirmar").performScrollTo().performClick()
    // Completed step (auto-navigated by AppNavHost to UEQ since it's the last task)
}

// ---------------------------------------------------------------------------
//  Complete entire Static condition (T1–T5)
// ---------------------------------------------------------------------------

fun MainComposeRule.completeStaticConditionTasks(
    userCode: String = "PACIENTE2391",
    pin: String = "928102"
) {
    completeT1AccessToUnlockTasks(userCode, pin)
    completeT2AppointmentForSequentialAccess()
    completeT3WellBeingForSequentialAccess()
    completeT4ReminderForSequentialAccess()
    completeT5Summary()
}

// ---------------------------------------------------------------------------
//  Complete entire Adaptive condition (T1–T5)
// ---------------------------------------------------------------------------

fun MainComposeRule.completeAdaptiveConditionTasks(
    userCode: String = "PACIENTE1830",
    pin: String = "830132"
) {
    completeT1AccessAdaptive(userCode, pin)
    completeT2AppointmentForSequentialAccess()
    completeT3WellBeingForSequentialAccess()
    completeT4ReminderForSequentialAccess()
    completeT5Summary()
}

// ---------------------------------------------------------------------------
//  UEQ – 26-item official questionnaire
// ---------------------------------------------------------------------------

/**
 * Answers all 26 UEQ items with value 4 (neutral midpoint) and submits.
 * Uses Siguiente navigation to advance through each page, then Guardar respuestas.
 */
fun MainComposeRule.completeOfficialUeq26() {
    // Wait for UEQ screen
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithText("UEQ").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Cuestionario completado").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("¡Gracias!").fetchSemanticsNodes().isNotEmpty()
    }

    // If already completed, acknowledge and return
    if (onAllNodesWithText("Cuestionario completado").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Continuar al siguiente paso").performScrollTo().performClick()
        return
    }
    if (onAllNodesWithText("¡Gracias!").fetchSemanticsNodes().isNotEmpty()) return

    // Answer all 26 items – wait for page text, click "4", wait for next page
    repeat(26) { page ->
        val currentItemText = "Ítem ${page + 1} de 26"
        val success = runCatching {
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(currentItemText).fetchSemanticsNodes().isNotEmpty() ||
                    onAllNodesWithText("Guardar respuestas").fetchSemanticsNodes().isNotEmpty() ||
                    onAllNodesWithText("¡Gracias!").fetchSemanticsNodes().isNotEmpty()
            }
        }.isSuccess
        if (!success) {
            onRoot().printToLog("UEQ_DEBUG")
            throw AssertionError("Timeout waiting for $currentItemText")
        }
        if (onAllNodesWithText("¡Gracias!").fetchSemanticsNodes().isNotEmpty()) return
        
        // Ensure EXACTLY 1 "4" is on screen (to avoid clicking the fading-out page's "4")
        waitUntil(timeoutMillis = 2_000) {
            onAllNodesWithText("4").fetchSemanticsNodes().size == 1
        }
        
        // Select neutral value 4
        onAllNodesWithText("4")[0].performClick()
        waitForIdle()
    }

    // Wait for Guardar respuestas button (on last item)
    waitUntil(timeoutMillis = 8_000) {
        onAllNodesWithText("Guardar respuestas").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("¡Gracias!").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithText("Guardar respuestas").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Guardar respuestas").performScrollTo().performClick()
    }

    // Wait for saved confirmation
    // Wait for saved confirmation
    val success = runCatching {
        waitUntil(timeoutMillis = 8_000) {
            onAllNodesWithText("¡Gracias!").fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText("Cuestionario completado").fetchSemanticsNodes().isNotEmpty()
        }
    }.isSuccess
    if (!success) {
        onAllNodesWithText("").fetchSemanticsNodes() // Just dummy to check if I can get nodes
        throw AssertionError("Timeout waiting for UEQ saved confirmation.")
    }
}

// ---------------------------------------------------------------------------
//  Condition transition
// ---------------------------------------------------------------------------

/**
 * After Static UEQ is saved, navigate to the condition transition screen and continue.
 */
fun MainComposeRule.continueToAdaptiveCondition() {
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithText("Primera etapa completada").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Continuar con la segunda interfaz").performScrollTo().performClick()
    waitForHome()
}

// ---------------------------------------------------------------------------
//  Interview
// ---------------------------------------------------------------------------

/**
 * Skips the interview by pressing "Omitir entrevista" and confirming.
 */
fun MainComposeRule.completeInterviewBySkippingWithConfirmation() {
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithText("Entrevista breve").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Entrevista guardada").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithText("Entrevista guardada").fetchSemanticsNodes().isNotEmpty()) return

    onAllNodesWithText("Omitir entrevista")[0].performScrollTo().performClick()
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("¿Omitir la entrevista?").fetchSemanticsNodes().isNotEmpty()
    }
    val omitirNodes = onAllNodesWithText("Omitir entrevista").fetchSemanticsNodes()
    if (omitirNodes.size > 1) {
        onAllNodesWithText("Omitir entrevista")[1].performClick()
    } else {
        onAllNodesWithText("Omitir entrevista")[0].performClick()
    }
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Entrevista guardada").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Sesión experimental completada", substring = true).fetchSemanticsNodes().isNotEmpty()
    }
}

// ---------------------------------------------------------------------------
//  Misc helpers
// ---------------------------------------------------------------------------

fun MainComposeRule.pressBackBestEffort(times: Int = 4) {
    runCatching {
        repeat(times) {
            pressBack()
            waitForIdle()
        }
    }
}