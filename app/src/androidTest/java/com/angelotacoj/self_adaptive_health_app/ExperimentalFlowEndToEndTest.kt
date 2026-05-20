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
    }

    @Test
    fun groupA_completesFirstConditionAndTransitions_staticToSelfAdaptive() {
        composeRule.startGroupASession("E2E_GROUP_A_001")

        composeRule.completeCurrentCondition(userCode = "PACIENTE01", pin = "1234", selfAdaptive = false)
        composeRule.onNodeWithText("Condición completada").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar con la siguiente condición").performScrollTo().performClick()
        composeRule.assertTextExists("SELF_ADAPTIVE_UI")
        composeRule.assertRoomCompletionCounts(total = 4, static = 4, selfAdaptive = 0)
    }

    @Test
    fun groupB_completesFirstConditionAndTransitions_selfAdaptiveToStatic() {
        composeRule.startGroupBSession("E2E_GROUP_B_001")

        composeRule.completeCurrentCondition(userCode = "PACIENTE02", pin = "5678", selfAdaptive = true)
        composeRule.onNodeWithText("Condición completada").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar con la siguiente condición").performScrollTo().performClick()
        composeRule.assertTextExists("STATIC_UI")
        composeRule.assertRoomCompletionCounts(total = 4, static = 0, selfAdaptive = 4)
    }

    @Test
    fun activeSession_persistsInRoomAndDataStoreForRecovery() {
        composeRule.startGroupASession("E2E_RESTORE_001")
        composeRule.completeT1Access(userCode = "PACIENTE01", pin = "1234", selfAdaptive = false)
        composeRule.assertRoomCompletionCounts(total = 1, static = 1, selfAdaptive = 0)
        composeRule.assertActiveSessionSnapshot(participantCode = "E2E_RESTORE_001")
        composeRule.assertTextExists("STATIC_UI")
        composeRule.onNodeWithText("T1 Acceder con código/PIN simulado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start_t1_access").performScrollTo().assertIsNotEnabled()
        composeRule.assertRoomCompletionCounts(total = 1, static = 1, selfAdaptive = 0)
    }
}

private fun MainComposeRule.completeCurrentCondition(
    userCode: String,
    pin: String,
    selfAdaptive: Boolean
) {
    completeT1Access(userCode, pin, selfAdaptive)
    completeT2WellBeing(selfAdaptive)
    completeT3Reminder(selfAdaptive)
    completeT4Summary(selfAdaptive)
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
        onNodeWithText("Confirmar").performClick()
    }
    onNodeWithText("Finalizar tarea").performScrollTo().performClick()
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT2WellBeing(selfAdaptive: Boolean) {
    openTaskByTitle("T2 Registro de bienestar")
    onNodeWithText("Iniciar formulario").performScrollTo().performClick()
    onNodeWithText("Validar valor").performScrollTo().performClick()
    onNodeWithText("Revisar antes de guardar").performScrollTo().performClick()
    onNodeWithText("Guardar").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar").performClick()
    }
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT3Reminder(selfAdaptive: Boolean) {
    openTaskByTitle("T3 Recordatorio")
    onNodeWithText("Crear recordatorio").performScrollTo().performClick()
    onNodeWithText("Usar esta actividad").performScrollTo().performClick()
    onNodeWithText("Usar esta hora").performScrollTo().performClick()
    onNodeWithText("Usar esta frecuencia").performScrollTo().performClick()
    onNodeWithText("Guardar recordatorio").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar").performClick()
    }
    returnHomeAfterNonFinalTask()
}

private fun MainComposeRule.completeT4Summary(selfAdaptive: Boolean) {
    openTaskByTitle("T4 Revisar y confirmar")
    onNodeWithText("Revisar detalles").performScrollTo().performClick()
    onNodeWithText("Guardar información").performScrollTo().performClick()
    if (selfAdaptive) {
        onNodeWithText("Confirmar").performClick()
    }
    onNodeWithText("Confirmar").performScrollTo().performClick()
}

private fun MainComposeRule.returnHomeAfterNonFinalTask() {
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
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

private fun MainComposeRule.assertNoParticipantTaskStartButtons() {
    TaskId.entries.forEach { task ->
        when (task) {
            TaskId.T1_ACCESS -> assertDoesNotExist("start_t1_access")
            TaskId.T2_WELL_BEING -> assertDoesNotExist("start_t2_wellbeing")
            TaskId.T3_REMINDER -> assertDoesNotExist("start_t3_reminder")
            TaskId.T4_SUMMARY -> assertDoesNotExist("start_t4_summary")
            else -> Unit
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.assertDoesNotExist(tag: String) {
    assert(onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) { "Expected no node with tag $tag." }
}

private data class CompletionCounts(
    val total: Int,
    val static: Int,
    val selfAdaptive: Int
)
