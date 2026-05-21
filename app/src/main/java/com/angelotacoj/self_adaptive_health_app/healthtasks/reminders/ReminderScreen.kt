package com.angelotacoj.self_adaptive_health_app.healthtasks.reminders

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
fun ReminderScreen(
    state: ReminderState,
    onAction: (ReminderAction) -> ReminderEvent?,
    onLog: (InteractionEventType, ScreenId, String) -> Unit,
    onAdaptiveEvent: (AdaptiveInteractionEventType, ScreenId) -> Boolean,
    onApplyAdaptation: () -> Unit,
    onRejectAdaptation: () -> Unit,
    onUndoAdaptation: () -> Unit,
    onHideHelp: () -> Unit,
    onKeepAdaptation: () -> Unit,
    onExit: () -> Unit
) {
    val screenId = state.step.toScreenId()
    BackHandler {
        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
        if (onAction(ReminderAction.BackClicked) is ReminderEvent.ExitTask) onExit()
    }
    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "Reminder step entered: $screenId.")
        if (AdaptiveTiming.prolongedTimeDetectionEnabled && state.step != ReminderStep.Intro) {
            delay(AdaptiveTiming.THRESHOLD_LONG)
            onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, screenId)
        }
    }

    ScreenContainer(
        title = "Recordatorio simulado",
        subtitle = "Configure un recordatorio que no activará notificaciones reales.",
        navigationLabel = "Volver",
        onNavigationClick = {
            onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
            if (onAction(ReminderAction.BackClicked) is ReminderEvent.ExitTask) onExit()
        },
        adaptiveUiState = state.adaptiveUiState
    ) {
        AdaptiveSuggestionCard(state.adaptiveUiState.pendingAdaptation, onApplyAdaptation, onRejectAdaptation, state.adaptiveUiState)
        AdaptiveConfirmationDialog(
            state.adaptiveUiState.pendingAdaptation,
            onConfirm = {
                onApplyAdaptation()
                if (state.step == ReminderStep.ReviewSummary) {
                    onAction(ReminderAction.SaveReminderClicked)
                    onLog(InteractionEventType.TASK_COMPLETED, ScreenId.REMINDER_SAVED, "T3 completed.")
                }
            },
            onEdit = onRejectAdaptation,
            onCancel = onRejectAdaptation,
            adaptiveUiState = state.adaptiveUiState
        )
        ContextualHelpBox(state.adaptiveUiState, onHideHelp)
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onKeepAdaptation, state.adaptiveUiState)

        when (state.step) {
            ReminderStep.Intro -> {
                TaskProgressHeader("Paso 1 de 6", "Introducción al recordatorio", adaptiveUiState = state.adaptiveUiState)
                InstructionCard(
                    if (state.adaptiveUiState.isAdaptiveMode) "Instrucciones de la tarea" else "Recordatorio asignado",
                    if (state.adaptiveUiState.isAdaptiveMode) {
                        listOf("Elija la actividad, hora y frecuencia asignadas.", "Revise el resumen antes de guardar.")
                    } else {
                        listOf("${state.activity}, ${state.time}, ${state.frequency}")
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                LargePrimaryButton("Crear recordatorio", { onAction(ReminderAction.StartNewReminderClicked) }, adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            ReminderStep.SelectActivity -> StepChoice("Paso 2 de 6", "Seleccionar actividad", "Actividad" to state.activity, "Usar esta actividad", state) {
                onAction(ReminderAction.ActivitySelected)
            }
            ReminderStep.SelectTime -> StepChoice("Paso 3 de 6", "Seleccionar hora", "Hora" to state.time, "Usar esta hora", state) {
                onAction(ReminderAction.TimeSelected)
            }
            ReminderStep.SelectFrequency -> StepChoice("Paso 4 de 6", "Seleccionar frecuencia", "Frecuencia" to state.frequency, "Usar esta frecuencia", state) {
                onAction(ReminderAction.FrequencySelected)
            }
            ReminderStep.ReviewSummary -> {
                TaskProgressHeader("Paso 5 de 6", "Revisar recordatorio", adaptiveUiState = state.adaptiveUiState)
                SummaryReviewCard("Resumen del recordatorio", listOf("Actividad" to state.activity, "Hora" to state.time, "Frecuencia" to state.frequency), adaptiveUiState = state.adaptiveUiState)
                ButtonRow(
                    primaryText = "Guardar recordatorio",
                    onPrimary = {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Attempted to save simulated reminder.")
                        val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId)
                        if (!requiresValidation) {
                            onAction(ReminderAction.SaveReminderClicked)
                            onLog(InteractionEventType.TASK_COMPLETED, ScreenId.REMINDER_SAVED, "T3 completed.")
                        }
                    },
                    secondaryText = "Volver",
                    onSecondary = { onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId); onAction(ReminderAction.BackClicked) },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.safeExitEnabled || !state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Cancelar", { if (onAction(ReminderAction.CancelClicked) is ReminderEvent.ExitTask) onExit() }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            ReminderStep.Saved -> {
                TaskProgressHeader("Paso 6 de 6", "Mensaje de éxito", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Recordatorio guardado", listOf("${state.activity} a las ${state.time}", state.frequency), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }
    }
}

@Composable
private fun StepChoice(
    step: String,
    title: String,
    row: Pair<String, String>,
    buttonText: String,
    state: ReminderState,
    onContinue: () -> Unit
) {
    TaskProgressHeader(step, title, adaptiveUiState = state.adaptiveUiState)
    SummaryReviewCard(title, listOf(row), adaptiveUiState = state.adaptiveUiState)
    LargePrimaryButton(buttonText, onContinue, adaptiveUiState = state.adaptiveUiState)
}

private fun ReminderStep.toScreenId(): ScreenId {
    return when (this) {
        ReminderStep.Intro -> ScreenId.REMINDER_INTRO
        ReminderStep.SelectActivity -> ScreenId.REMINDER_ACTIVITY
        ReminderStep.SelectTime -> ScreenId.REMINDER_TIME
        ReminderStep.SelectFrequency -> ScreenId.REMINDER_FREQUENCY
        ReminderStep.ReviewSummary -> ScreenId.REMINDER_REVIEW
        ReminderStep.Saved -> ScreenId.REMINDER_SAVED
    }
}
