package com.angelotacoj.self_adaptive_health_app.healthtasks.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptiveTiming
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.AdaptiveConfirmationDialog
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.AdaptiveSuggestionCard
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.ContextualHelpBox
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.UndoAdaptationCard
import com.angelotacoj.self_adaptive_health_app.core.logging.InteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.ui.ButtonRow
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.SummaryReviewCard
import com.angelotacoj.self_adaptive_health_app.core.ui.TaskProgressHeader

@Composable
fun SummaryScreen(
    state: SummaryState,
    onAction: (SummaryAction) -> SummaryEvent?,
    onLog: (InteractionEventType, ScreenId, String) -> Unit,
    onAdaptiveEvent: (AdaptiveInteractionEventType, ScreenId, com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary?) -> Boolean,
    onApplyAdaptation: () -> Unit,
    onRejectAdaptation: () -> Unit,
    onUndoAdaptation: () -> Unit,
    onHideHelp: () -> Unit,
    onKeepAdaptation: () -> Unit,
    onExit: () -> Unit
) {
    val screenId = state.step.toScreenId()
    BackHandler {
        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId, null)
        if (onAction(SummaryAction.BackClicked) is SummaryEvent.ExitTask) onExit()
    }
    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "Summary step entered: $screenId.")
        if (AdaptiveTiming.prolongedTimeDetectionEnabled && state.step != SummaryStep.Intro) {
            delay(AdaptiveTiming.getThresholdForScreen(screenId))
            onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, screenId, null)
        }
    }
    if (state.step == SummaryStep.ReinforcedConfirmation) {
        LaunchedEffect(screenId) {
            delay(AdaptiveTiming.getConfirmationPauseThreshold())
            onLog(InteractionEventType.CONFIRMATION_PAUSE, screenId, "Confirmation pause reached.")
            onAdaptiveEvent(AdaptiveInteractionEventType.CONFIRMATION_PAUSE, screenId, null)
        }
    }

    ScreenContainer(
        title = "Revisar información",
        subtitle = "Revise la información simulada antes de cualquier confirmación final.",
        navigationLabel = "Volver",
        onNavigationClick = {
            onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId, null)
            if (onAction(SummaryAction.BackClicked) is SummaryEvent.ExitTask) onExit()
        },
        adaptiveUiState = state.adaptiveUiState
    ) {
        AdaptiveSuggestionCard(state.adaptiveUiState.pendingAdaptation, onApplyAdaptation, onRejectAdaptation, state.adaptiveUiState)
        AdaptiveConfirmationDialog(
            state.adaptiveUiState.pendingAdaptation,
            onConfirm = {
                onApplyAdaptation()
                when (state.step) {
                    SummaryStep.Details -> onAction(SummaryAction.SaveInformationClicked)
                    SummaryStep.ReinforcedConfirmation -> {
                        onAction(SummaryAction.ConfirmClicked)
                        onLog(InteractionEventType.TASK_COMPLETED, ScreenId.SUMMARY_FINAL, "T5 completed with confirmation.")
                    }
                    else -> Unit
                }
            },
            onEdit = { onAction(SummaryAction.EditClicked) },
            onCancel = onRejectAdaptation,
            adaptiveUiState = state.adaptiveUiState
        )
        ContextualHelpBox(state.adaptiveUiState, onHideHelp)
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onKeepAdaptation, state.adaptiveUiState)

        val parsedItems = androidx.compose.runtime.remember(state.taskOutputs) {
            val list = mutableListOf<Pair<String, String>>()
            
            val t1 = state.taskOutputs["T1_ACCESS"]
            if (t1 != null) {
                val json = org.json.JSONObject(t1)
                list.add("T1: Acceso" to "Código: ${json.optString("participantCode", "N/A")}")
            } else {
                list.add("T1: Acceso" to "Información simulada no registrada aún.")
            }

            val t2 = state.taskOutputs["T2_APPOINTMENT"]
            if (t2 != null) {
                val json = org.json.JSONObject(t2)
                list.add("T2: Cita" to "${json.optString("professionalName", "")} - ${json.optString("appointmentDate", "")} ${json.optString("appointmentTime", "")}")
            } else {
                list.add("T2: Cita" to "Información simulada no registrada aún.")
            }

            val t3 = state.taskOutputs["T3_WELL_BEING"]
            if (t3 != null) {
                val json = org.json.JSONObject(t3)
                list.add("T3: Bienestar" to "Energía: ${json.optString("energyLevel", "")}, Ánimo: ${json.optString("mood", "")}")
            } else {
                list.add("T3: Bienestar" to "Información simulada no registrada aún.")
            }

            val t4 = state.taskOutputs["T4_REMINDER"]
            if (t4 != null) {
                val json = org.json.JSONObject(t4)
                list.add("T4: Recordatorio" to "${json.optString("reminderType", "")} a las ${json.optString("reminderTime", "")}")
            } else {
                list.add("T4: Recordatorio" to "Información simulada no registrada aún.")
            }
            list
        }

        when (state.step) {
            SummaryStep.Intro -> {
                TaskProgressHeader("Paso 1 de 4", "Resumen de información", adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    InstructionCard("Instrucciones de la tarea", listOf("Revise el acceso y el recordatorio simulados.", "Guardar puede mostrar una confirmación reforzada."), adaptiveUiState = state.adaptiveUiState)
                }
                LargePrimaryButton("Revisar detalles", { onAction(SummaryAction.StartReviewClicked) }, adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId, null) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            SummaryStep.Details -> {
                TaskProgressHeader("Paso 2 de 4", "Revisar detalles", adaptiveUiState = state.adaptiveUiState)
                SummaryReviewCard(
                    "Datos simulados",
                    parsedItems,
                    adaptiveUiState = state.adaptiveUiState
                )
                LargePrimaryButton(
                    "Guardar información",
                    {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Save information clicked for simulated summary.")
                        val summaryData = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary(
                            title = "Datos simulados",
                            details = parsedItems.toMap()
                        )
                        val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId, summaryData)
                        if (!requiresValidation) onAction(SummaryAction.SaveInformationClicked)
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId, null) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            SummaryStep.ReinforcedConfirmation -> {
                TaskProgressHeader("Paso 3 de 4", if (state.adaptiveUiState.reinforcedConfirmationVisible) "Confirmación reforzada" else "Confirmación", adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.reinforcedConfirmationVisible || state.adaptiveUiState.contextualHelpVisible) {
                    InstructionCard("Antes de guardar", listOf("Está por confirmar información simulada.", "Puede confirmar, editar o cancelar."), adaptiveUiState = state.adaptiveUiState)
                    SummaryReviewCard(
                        "Resumen de lo que se guardará",
                        parsedItems,
                        adaptiveUiState = state.adaptiveUiState
                    )
                }
                ButtonRow(
                    primaryText = "Confirmar",
                    onPrimary = {
                        onAction(SummaryAction.ConfirmClicked)
                        onLog(InteractionEventType.TASK_COMPLETED, ScreenId.SUMMARY_FINAL, "T5 completed with confirmation.")
                    },
                    secondaryText = "Editar",
                    onSecondary = { onAction(SummaryAction.EditClicked) },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.safeExitEnabled || !state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Cancelar", { onAction(SummaryAction.CancelClicked); onLog(InteractionEventType.TASK_COMPLETED, ScreenId.SUMMARY_FINAL, "T5 completed with cancel.") }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            SummaryStep.Final -> {
                TaskProgressHeader("Paso 4 de 4", "Mensaje final", adaptiveUiState = state.adaptiveUiState)
                val resultText = when (state.result) {
                    SummaryResult.Confirmed -> "La información simulada fue confirmada."
                    SummaryResult.Edited -> "Se seleccionó editar."
                    SummaryResult.Cancelled -> "La revisión fue cancelada."
                    null -> "La tarea ha finalizado."
                }
                InstructionCard("Tarea finalizada", listOf(resultText, state.editNote).filter { it.isNotBlank() }, adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }
    }
}

private fun SummaryStep.toScreenId(): ScreenId {
    return when (this) {
        SummaryStep.Intro -> ScreenId.SUMMARY_INTRO
        SummaryStep.Details -> ScreenId.SUMMARY_REVIEW
        SummaryStep.ReinforcedConfirmation -> ScreenId.SUMMARY_CONFIRMATION
        SummaryStep.Final -> ScreenId.SUMMARY_FINAL
    }
}
