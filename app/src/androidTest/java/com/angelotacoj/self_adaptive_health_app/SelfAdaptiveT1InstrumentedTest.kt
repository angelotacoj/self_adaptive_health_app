package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelfAdaptiveT1InstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppData() {
        composeRule.resetResearchData()
    }

    @After
    fun leaveTaskBeforeActivityDestroy() {
        runCatching {
            repeat(3) {
                composeRule.activity.runOnUiThread {
                    composeRule.activity.onBackPressedDispatcher.onBackPressed()
                }
                composeRule.waitForIdle()
            }
        }
    }

    @Test
    fun t1_ar02_prolongedTimeInSelfAdaptiveUi_appliesAndUndoRestoresTextScale() {
        composeRule.startGroupBSession("UI_AR02_001")
        openT1CodeStep()

        composeRule.waitUntil(timeoutMillis = 35_000) {
            composeRule.onAllNodesWithText("Cambio aplicado automáticamente").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Cambio aplicado automáticamente").assertIsDisplayed()
        composeRule.onNodeWithText("Ayuda del sistema").assertIsDisplayed()

        composeRule.onNodeWithText("Deshacer cambio").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Entendido. No volveré a mostrar esta sugerencia durante esta tarea.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Entendido. No volveré a mostrar esta sugerencia durante esta tarea.").assertIsDisplayed()
        exitAccessTask()
    }

    @Test
    fun t1_helpRequestInSelfAdaptiveUi_showsVisibleHelp() {
        composeRule.startGroupBSession("UI_AR05_001")
        openT1CodeStep()

        composeRule.onNodeWithText("Necesito ayuda").performScrollTo().performClick()

        composeRule.onNodeWithText("Ayuda para acceder").assertIsDisplayed()
        composeRule.onNodeWithText("Use exactamente el código ficticio que aparece en la tarjeta de credenciales.").assertIsDisplayed()
        composeRule.onNodeWithText("Entendido").performClick()
        exitAccessTask()
    }

    @Test
    fun t1_fieldErrorInSelfAdaptiveUi_showsSpanishRecoveryMessage() {
        composeRule.startGroupBSession("UI_AR06_001")
        openT1CodeStep()

        composeRule.onNodeWithText("Continuar").performScrollTo().performClick()

        composeRule.onNodeWithText("Ingrese el código de usuario mostrado en pantalla.").performScrollTo().assertIsDisplayed()
        exitAccessTask()
    }

    @Test
    fun t1_sensitiveActionInSelfAdaptiveUi_showsExplicitValidation() {
        composeRule.startGroupBSession("UI_AR08_001")
        openT1CodeStep()

        composeRule.onNode(editableTextField("Código de usuario")).performTextInput("PACIENTE02")
        composeRule.onNodeWithText("Continuar").performScrollTo().performClick()
        composeRule.onNode(editableTextField("PIN simulado")).performTextInput("5678")
        composeRule.onNodeWithText("Validar acceso").performScrollTo().performClick()

        composeRule.onNodeWithText("Revisar antes de continuar").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmar y continuar").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        exitAccessTask()
    }

    private fun openT1CodeStep() {
        composeRule.openTaskByTitle("T1 Acceder con código/PIN simulado")
        composeRule.onNodeWithText("Comenzar").performScrollTo().performClick()
        composeRule.onNodeWithText("Paso 2 de 5").assertIsDisplayed()
    }

    private fun exitAccessTask() {
        composeRule.pressBackBestEffort()
    }
}
