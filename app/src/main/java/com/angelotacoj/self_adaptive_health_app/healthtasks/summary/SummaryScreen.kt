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
    onExit: () -> Unit
) {
    val screenId = state.step.toScreenId()
    BackHandler {
        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId, null)
        if (onAction(SummaryAction.BackClicked) is SummaryEvent.ExitTask) onExit()
    }
    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "Summary step entered: $screenId.")
        if (AdaptiveTiming.prolongedTimeDetectionEnabled) {
            delay(AdaptiveTiming.THRESHOLD_SHORT)
            onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, ScreenId.SUMMARY_INTRO, null)
        }
    }
    if (state.step == SummaryStep.ReinforcedConfirmation) {
        LaunchedEffect(screenId) {
            delay(12_000)
            onLog(InteractionEventType.CONFIRMATION_PAUSE, screenId, "Confirmation pause reached 12 seconds.")
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
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onHideHelp, state.adaptiveUiState)

        when (state.step) {
            SummaryStep.Intro -> {
                TaskProgressHeader("Paso 1 de 4", "Resumen de información")
                InstructionCard("Instrucciones de la tarea", listOf("Revise el acceso y el recordatorio simulados.", "Guardar requiere una confirmación reforzada."))
                LargePrimaryButton("Revisar detalles", { onAction(SummaryAction.StartReviewClicked) }, adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId, null) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            SummaryStep.Details -> {
                TaskProgressHeader("Paso 2 de 4", "Revisar detalles")
                SummaryReviewCard(
                    "Datos simulados",
                    listOf(
                        "Acceso" to "Código ${state.dataSet.accessCredentials.userCode}",
                        "Recordatorio" to state.dataSet.reminder.time
                    )
                )
                LargePrimaryButton(
                    "Guardar información",
                    {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Save information clicked for simulated summary.")
                        val summaryData = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary(
                            title = "Datos simulados",
                            details = mapOf(
                                "Acceso" to "Código ${state.dataSet.accessCredentials.userCode}",
                                "Recordatorio" to state.dataSet.reminder.time
                            )
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
                TaskProgressHeader("Paso 3 de 4", "Confirmación reforzada")
                InstructionCard("Antes de guardar", listOf("Está por confirmar información simulada.", "Elija Confirmar, Editar o Cancelar."))
                ButtonRow(
                    primaryText = "Confirmar",
                    onPrimary = {
                        onAction(SummaryAction.ConfirmClicked)
                        onLog(InteractionEventType.TASK_COMPLETED, ScreenId.SUMMARY_FINAL, "T5 completed with confirmation.")
                    },
                    secondaryText = "Editar",
                    onSecondary = { onAction(SummaryAction.EditClicked) }
                )
                LargeSecondaryButton("Cancelar", { onAction(SummaryAction.CancelClicked); onLog(InteractionEventType.TASK_COMPLETED, ScreenId.SUMMARY_FINAL, "T5 completed with cancel.") }, adaptiveUiState = state.adaptiveUiState)
            }
            SummaryStep.Final -> {
                TaskProgressHeader("Paso 4 de 4", "Mensaje final")
                val resultText = when (state.result) {
                    SummaryResult.Confirmed -> "La información simulada fue confirmada."
                    SummaryResult.Edited -> "Se seleccionó editar."
                    SummaryResult.Cancelled -> "La revisión fue cancelada."
                    null -> "La tarea ha finalizado."
                }
                InstructionCard("Tarea finalizada", listOf(resultText, state.editNote).filter { it.isNotBlank() })
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
