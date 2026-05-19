package com.angelotacoj.self_adaptive_health_app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelfAdaptiveTasksInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppData() {
        composeRule.resetResearchData()
    }

    @After
    fun leaveTaskBeforeActivityDestroy() {
        composeRule.pressBackBestEffort()
    }

    @Test
    fun t2_fieldErrorInSelfAdaptiveUi_showsAr06RecoveryMessage() {
        composeRule.startGroupBSession("UI_T2_AR06_001")
        composeRule.openTaskByTitle("T2 Registro de bienestar")
        composeRule.onNodeWithText("Iniciar formulario").performScrollTo().performClick()

        composeRule.onNode(editableTextField("Nivel de energía")).performTextClearance()
        composeRule.onNodeWithText("Validar valor").performScrollTo().performClick()

        composeRule.onNodeWithText("Ingrese un valor ficticio del 1 al 10.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ayuda para continuar").assertIsDisplayed()
    }

    @Test
    fun t2_sensitiveSaveInSelfAdaptiveUi_showsExplicitValidation() {
        composeRule.startGroupBSession("UI_T2_AR08_001")
        composeRule.openTaskByTitle("T2 Registro de bienestar")
        composeRule.onNodeWithText("Iniciar formulario").performScrollTo().performClick()
        composeRule.onNodeWithText("Validar valor").performScrollTo().performClick()
        composeRule.onNodeWithText("Revisar antes de guardar").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar").performScrollTo().performClick()

        composeRule.onNodeWithText("Revisar antes de continuar").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmar").assertIsDisplayed()
    }

    @Test
    fun t3_backtrackingInSelfAdaptiveUi_showsAr04Suggestion() {
        composeRule.startGroupBSession("UI_T3_AR04_001")
        composeRule.openTaskByTitle("T3 Recordatorio")
        composeRule.onNodeWithText("Crear recordatorio").performScrollTo().performClick()
        composeRule.onNodeWithText("Usar esta actividad").performScrollTo().performClick()
        composeRule.onNodeWithText("Usar esta hora").performScrollTo().performClick()

        pressBack()
        composeRule.waitForIdle()
        pressBack()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Activar guía paso a paso").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Activar guía paso a paso").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar cambio").assertIsDisplayed()
    }

    @Test
    fun t3_sensitiveSaveInSelfAdaptiveUi_showsExplicitValidation() {
        composeRule.startGroupBSession("UI_T3_AR08_001")
        composeRule.openTaskByTitle("T3 Recordatorio")
        composeRule.onNodeWithText("Crear recordatorio").performScrollTo().performClick()
        composeRule.onNodeWithText("Usar esta actividad").performScrollTo().performClick()
        composeRule.onNodeWithText("Usar esta hora").performScrollTo().performClick()
        composeRule.onNodeWithText("Usar esta frecuencia").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar recordatorio").performScrollTo().performClick()

        composeRule.onNodeWithText("Revisar antes de continuar").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmar").assertIsDisplayed()
    }

    @Test
    fun t4_sensitiveSaveInSelfAdaptiveUi_showsExplicitValidation() {
        composeRule.startGroupBSession("UI_T4_AR08_001")
        composeRule.openTaskByTitle("T4 Revisar y confirmar")
        composeRule.onNodeWithText("Revisar detalles").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar información").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Revisar antes de continuar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Revisar antes de continuar").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmar").assertIsDisplayed()
    }

    @Test
    fun t4_confirmationPauseInSelfAdaptiveUi_showsAr03ExplicitValidation() {
        composeRule.startGroupBSession("UI_T4_AR03_001")
        composeRule.openTaskByTitle("T4 Revisar y confirmar")
        composeRule.onNodeWithText("Revisar detalles").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar información").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Confirmar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Confirmar").performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Revisar antes de guardar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Revisar antes de guardar").assertIsDisplayed()
    }
}
