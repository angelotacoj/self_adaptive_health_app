package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptiveTiming
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import kotlinx.coroutines.runBlocking

typealias MainComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

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
}

fun MainComposeRule.startGroupBSession(participantCode: String) {
    onNodeWithText("Código de participante").performTextClearance()
    onNodeWithText("Código de participante").performTextInput(participantCode)
    onNodeWithText("Grupo B").performScrollTo().performClick()
    onNodeWithText("Iniciar sesión experimental").performScrollTo().performClick()
    onNodeWithText("Etapa 1").assertExistsCompat()
}

fun MainComposeRule.startGroupASession(participantCode: String) {
    onNodeWithText("Código de participante").performTextClearance()
    onNodeWithText("Código de participante").performTextInput(participantCode)
    onNodeWithText("Grupo A").performScrollTo().performClick()
    onNodeWithText("Iniciar sesión experimental").performScrollTo().performClick()
    onNodeWithText("Etapa 1").assertExistsCompat()
}

fun MainComposeRule.openTaskByTitle(title: String) {
    onNodeWithText(title).performScrollTo()
    val tag = when {
        title.startsWith("T1") -> "start_t1_access"
        title.startsWith("T2") -> "start_t2_appointment"
        title.startsWith("T3") -> "start_t3_wellbeing"
        title.startsWith("T4") -> "start_t4_reminder"
        title.startsWith("T5") -> "start_t5_summary"
        else -> error("Unknown task title: $title")
    }
    onNodeWithTag(tag).performScrollTo().performClick()
}

fun MainComposeRule.completeT1AccessToUnlockTasks() {
    openTaskByTitle("T1 Acceder con código/PIN simulado")
    onNodeWithText("Comenzar").performScrollTo().performClick()
    onNode(editableTextField("Código de usuario")).performTextInput("PACIENTE02")
    onNodeWithText("Continuar").performScrollTo().performClick()
    onNode(editableTextField("PIN simulado")).performTextInput("5678")
    onNodeWithText("Validar acceso").performScrollTo().performClick()
    onNodeWithText("Confirmar y continuar").performClick()
    onNodeWithText("Finalizar tarea").performScrollTo().performClick()
    onNodeWithText("Volver al inicio").performScrollTo().performClick()
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Inicio").fetchSemanticsNodes().isNotEmpty()
    }
}

fun MainComposeRule.pressBackBestEffort(times: Int = 4) {
    runCatching {
        repeat(times) {
            pressBack()
            waitForIdle()
        }
    }
}

fun editableTextField(label: String): SemanticsMatcher {
    return hasSetTextAction() and hasText(label, substring = true)
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() {
    fetchSemanticsNode()
}
