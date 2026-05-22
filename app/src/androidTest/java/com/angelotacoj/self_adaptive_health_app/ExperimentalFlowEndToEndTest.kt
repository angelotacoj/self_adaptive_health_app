package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptiveTiming
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

    @Test
    fun groupA_completesFirstConditionAndTransitions_staticToSelfAdaptive() {
        composeRule.startGroupASession("E2E_GROUP_A_001")

        composeRule.completeCurrentCondition(userCode = "PACIENTE01", pin = "1234", selfAdaptive = false)
        composeRule.onNodeWithText("Bloque de tareas completado").assertIsDisplayed()
        composeRule.onNodeWithText("UEQ-S completado, continuar").performScrollTo().performClick()
        composeRule.assertTextExists("Etapa 2")
        composeRule.assertRoomCompletionCounts(total = 5, static = 5, selfAdaptive = 0)
    }

    @Test
    fun groupB_completesFirstConditionAndTransitions_selfAdaptiveToStatic() {
        composeRule.startGroupBSession("E2E_GROUP_B_001")

        composeRule.completeCurrentCondition(userCode = "PACIENTE02", pin = "5678", selfAdaptive = true)
        composeRule.onNodeWithText("Bloque de tareas completado").assertIsDisplayed()
        composeRule.onNodeWithText("UEQ-S completado, continuar").performScrollTo().performClick()
        composeRule.assertTextExists("Etapa 2")
        composeRule.assertRoomCompletionCounts(total = 5, static = 0, selfAdaptive = 5)
    }

    @Test
    fun activeSession_persistsInRoomAndDataStoreForRecovery() {
        val participantCode = composeRule.startGroupASession("E2E_RESTORE_001")
        composeRule.completeT1Access(userCode = "PACIENTE01", pin = "1234", selfAdaptive = false)
        composeRule.assertRoomCompletionCounts(total = 1, static = 1, selfAdaptive = 0)
        composeRule.assertActiveSessionSnapshot(participantCode = participantCode)
        composeRule.assertTextExists("Etapa 1")
        composeRule.onNodeWithText("T1 Acceder con código/PIN simulado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start_t1_access").performScrollTo().assertIsNotEnabled()
        composeRule.assertRoomCompletionCounts(total = 1, static = 1, selfAdaptive = 0)
    }

    @Test
    fun staticConditionDoesNotShowAdaptiveHelpAffordances() {
        composeRule.startGroupASession("E2E_STATIC_NO_HELP_001")

        composeRule.assertTextAbsent("Ayuda: explicar esta sesión")
        composeRule.onNodeWithTag("start_t2_appointment").performScrollTo().assertIsNotEnabled()
        composeRule.openTaskByTitle("T1 Acceder con código/PIN simulado")
        composeRule.onNodeWithText("Comenzar").performScrollTo().performClick()
        composeRule.assertTextExists("Paso 2 de 5")
        composeRule.assertTextAbsent("Necesito ayuda")
        composeRule.onNodeWithText("Cancelar tarea").performScrollTo().performClick()
        composeRule.waitForHome()

        composeRule.openTaskByTitle("T3 Registro de bienestar")
        composeRule.waitForIdle()

        composeRule.assertTextAbsent("Necesito ayuda")
        composeRule.assertTextAbsent("Ayuda del sistema")
        composeRule.assertTextAbsent("Sugerencia de adaptación")
        composeRule.assertTextAbsent("Cambio aplicado automáticamente")
    }
}

private fun MainComposeRule.completeCurrentCondition(
    userCode: String,
    pin: String,
    selfAdaptive: Boolean
) {
    completeT1Access(userCode, pin, selfAdaptive)
    completeT2Appointment(selfAdaptive)
    completeT3WellBeing(selfAdaptive)
    completeT4Reminder(selfAdaptive)
    completeT5Summary(selfAdaptive)
}

private fun MainComposeRule.completeT1Access(
    userCode: String,
    pin: String,
    selfAdaptive: Boolean
) {
    openTaskByTitle("T1 Acceder con código/PIN simulado")
    onNodeWithText("Comenzar").performScrollTo().performClick()
    onNode(editableTextField("Código de usuario")).performTextInput(userCode)
    onNodeWithText("Continuar").performScrollTo().performClick()
    onNode(editableTextField("PIN simulado")).performTextInput(pin)
    onNodeWithText("Validar acceso").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar y continuar").performClick()
    }
    onNodeWithText("Finalizar tarea").performScrollTo().performClick()
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT2Appointment(selfAdaptive: Boolean) {
    openTaskByTitle("T2 Consultar cita médica")
    onNodeWithText("Ver lista de citas").performScrollTo().performClick()
    // Select any appointment (the first one)
    onAllNodesWithText("Abrir", substring = true)[0].performScrollTo().performClick()
    onNodeWithText("Continuar a confirmación").performScrollTo().performClick()
    onNodeWithText("Sí, continuar").performScrollTo().performClick()
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT3WellBeing(selfAdaptive: Boolean) {
    openTaskByTitle("T3 Registro de bienestar")
    onNodeWithText("Iniciar formulario").performScrollTo().performClick()
    onNode(wellBeingValueField()).performTextInput("5")
    onNodeWithText("Validar valor").performScrollTo().performClick()
    onNodeWithText("Revisar antes de guardar").performScrollTo().performClick()
    onNodeWithText("Guardar").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar y continuar").performClick()
    }
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT4Reminder(selfAdaptive: Boolean) {
    openTaskByTitle("T4 Recordatorio")
    onNodeWithText("Crear recordatorio").performScrollTo().performClick()
    onNodeWithText("Usar esta actividad").performScrollTo().performClick()
    onNodeWithText("Usar esta hora").performScrollTo().performClick()
    onNodeWithText("Usar esta frecuencia").performScrollTo().performClick()
    onNodeWithText("Guardar recordatorio").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar y continuar").performClick()
    }
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT5Summary(selfAdaptive: Boolean) {
    openTaskByTitle("T5 Revisar y confirmar")
    onNodeWithText("Revisar detalles").performScrollTo().performClick()
    onNodeWithText("Guardar información").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar y continuar").performClick()
    }
    onNodeWithText("Confirmar").performScrollTo().performClick()
    // Wait for navigation to ConditionTransition or next step
    waitForIdle()
}

private fun MainComposeRule.returnHomeAfterNonFinalTask() {
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitForHome()
}

private fun MainComposeRule.waitForHome() {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Inicio").fetchSemanticsNodes().isNotEmpty()
    }
}

private fun MainComposeRule.assertRoomCompletionCounts(
    total: Int,
    static: Int,
    selfAdaptive: Int
) {
    waitUntil(timeoutMillis = 10_000) {
        val counts = roomCompletionCounts()
        counts.total == total && counts.static == static && counts.selfAdaptive == selfAdaptive
    }
}

private fun MainComposeRule.assertActiveSessionSnapshot(participantCode: String) {
    waitUntil(timeoutMillis = 10_000) {
        runBlocking {
            val snapshot = AppContainer.experimentPreferences.sessionSnapshot.first()
            val session = snapshot.currentSessionId?.let { AppContainer.database.experimentDao().getSessionById(it) }
            snapshot.isSessionActive &&
                session?.participantCode == participantCode &&
                AppContainer.database.experimentDao().getTotalCompletedTaskCount(session.sessionId) == 1
        }
    }
}

private fun MainComposeRule.assertTextExists(text: String) {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

private fun MainComposeRule.assertTextAbsent(text: String) {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    }
}

private fun roomCompletionCounts(): CompletionCounts {
    return runBlocking {
        val dao = AppContainer.database.experimentDao()
        val sessionId = dao.getActiveSession()?.sessionId
            ?: dao.getSessionByParticipantCode("E2E_GROUP_A_001").firstOrNull()?.sessionId
            ?: dao.getSessionByParticipantCode("E2E_GROUP_B_001").firstOrNull()?.sessionId
            ?: dao.getSessionByParticipantCode("E2E_RESTORE_001").firstOrNull()?.sessionId
            ?: return@runBlocking CompletionCounts(0, 0, 0)
        CompletionCounts(
            total = dao.getTotalCompletedTaskCount(sessionId),
            static = dao.getCompletedTaskCount(sessionId, "STATIC_UI"),
            selfAdaptive = dao.getCompletedTaskCount(sessionId, "SELF_ADAPTIVE_UI")
        )
    }
}

private data class CompletionCounts(
    val total: Int,
    val static: Int,
    val selfAdaptive: Int
)
