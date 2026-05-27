package com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
fun WellBeingScreen(
    state: WellBeingState,
    onAction: (WellBeingAction) -> WellBeingEvent?,
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
        if (onAction(WellBeingAction.BackClicked) is WellBeingEvent.ExitTask) onExit()
    }
    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "WellBeing step entered: $screenId.")
        if (AdaptiveTiming.prolongedTimeDetectionEnabled) {
            delay(AdaptiveTiming.getThresholdForScreen(screenId))
            onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, screenId)
        }
    }

    ScreenContainer(
        title = "Registro de bienestar",
        subtitle = "Ingrese un valor ficticio. No es información clínica.",
        navigationLabel = "Volver",
        onNavigationClick = {
            onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
            if (onAction(WellBeingAction.BackClicked) is WellBeingEvent.ExitTask) onExit()
        },
        adaptiveUiState = state.adaptiveUiState
    ) {
        AdaptiveSuggestionCard(state.adaptiveUiState.pendingAdaptation, onApplyAdaptation, onRejectAdaptation, state.adaptiveUiState)
        AdaptiveConfirmationDialog(
            state.adaptiveUiState.pendingAdaptation,
            onConfirm = {
                onApplyAdaptation()
                if (state.step == WellBeingStep.Review) {
                    onAction(WellBeingAction.SaveClicked)
                    onLog(InteractionEventType.TASK_COMPLETED, ScreenId.WELL_BEING_SUCCESS, "T3 completed.")
                }
            },
            onEdit = { onAction(WellBeingAction.EditClicked) },
            onCancel = onRejectAdaptation,
            adaptiveUiState = state.adaptiveUiState
        )
        ContextualHelpBox(state.adaptiveUiState, onHideHelp)
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onKeepAdaptation, state.adaptiveUiState)

        when (state.step) {
            WellBeingStep.Intro -> {
                TaskProgressHeader("Paso 1 de 5", "Introducción", adaptiveUiState = state.adaptiveUiState)
                InstructionCard(
                    if (state.adaptiveUiState.isAdaptiveMode) "Instrucciones de la tarea" else "Dato asignado",
                    if (state.adaptiveUiState.isAdaptiveMode) {
                        listOf(
                            "Use el valor ficticio asignado.",
                            "${state.label}: ${state.suggestedValue}",
                            "Rango aceptado: 1 a 10."
                        )
                    } else {
                        listOf("${state.label}: ${state.suggestedValue}")
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                LargePrimaryButton("Iniciar formulario", { onAction(WellBeingAction.StartFormClicked) }, adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            WellBeingStep.Form -> {
                TaskProgressHeader("Paso 2 de 5", "Formulario de datos ficticios", adaptiveUiState = state.adaptiveUiState)
                OutlinedTextField(
                    value = state.valueText,
                    onValueChange = { onAction(WellBeingAction.ValueChanged(it)) },
                    label = { Text(state.label) },
                    supportingText = { Text(if (state.adaptiveUiState.contextualHelpVisible) "Ingrese un número del 1 al 10. Ejemplo: ${state.suggestedValue}" else "Número del 1 al 10") },
                    textStyle = if (state.adaptiveUiState.isAdaptiveMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                    isError = state.errorMessage != null
                )
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    if (state.adaptiveUiState.contextualHelpVisible || state.fieldErrorCount >= 2) Text("Ejemplo: ingrese ${state.suggestedValue}.", style = MaterialTheme.typography.bodyLarge)
                }
                ButtonRow(
                    primaryText = "Validar valor",
                    onPrimary = {
                        val value = state.valueText.toIntOrNull()
                        if (value == null || value !in 1..10) {
                            onLog(InteractionEventType.FIELD_ERROR, screenId, "Invalid fictitious value entered. Exact value not logged.")
                            onAdaptiveEvent(AdaptiveInteractionEventType.FIELD_ERROR, screenId)
                        }
                        onAction(WellBeingAction.ValidateClicked)
                    },
                    secondaryText = if (state.adaptiveUiState.safeExitEnabled) "Cancelar" else "Atrás",
                    onSecondary = {
                        val event = if (state.adaptiveUiState.safeExitEnabled) {
                            onAction(WellBeingAction.CancelClicked)
                        } else {
                            onAction(WellBeingAction.BackClicked)
                        }
                        if (event is WellBeingEvent.ExitTask) onExit()
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
            }
            WellBeingStep.Validation -> {
                TaskProgressHeader("Paso 3 de 5", "Validación", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Valor aceptado", listOf("El valor ficticio está dentro del rango aceptado.", "Continúe para revisar antes de guardar."), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Revisar antes de guardar", { onAction(WellBeingAction.ContinueToReviewClicked) }, adaptiveUiState = state.adaptiveUiState)
            }
            WellBeingStep.Review -> {
                TaskProgressHeader("Paso 4 de 5", "Revisión antes de guardar", adaptiveUiState = state.adaptiveUiState)
                SummaryReviewCard("Esta información es simulada", listOf(state.label to state.valueText), adaptiveUiState = state.adaptiveUiState)
                ButtonRow(
                    primaryText = "Guardar",
                    onPrimary = {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Attempted to save simulated well-being record. Exact value not logged.")
                        val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId)
                        if (!requiresValidation) {
                            onAction(WellBeingAction.SaveClicked)
                            onLog(InteractionEventType.TASK_COMPLETED, ScreenId.WELL_BEING_SUCCESS, "T3 completed.")
                        }
                    },
                    secondaryText = "Editar",
                    onSecondary = { onAction(WellBeingAction.EditClicked) },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.safeExitEnabled || !state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Cancelar", { if (onAction(WellBeingAction.CancelClicked) is WellBeingEvent.ExitTask) onExit() }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            WellBeingStep.Success -> {
                TaskProgressHeader("Paso 5 de 5", "Mensaje de éxito", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Registro ficticio guardado", listOf("Este valor simulado no fue almacenado como dato clínico."), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }
    }
}

private fun WellBeingStep.toScreenId(): ScreenId {
    return when (this) {
        WellBeingStep.Intro -> ScreenId.WELL_BEING_INTRO
        WellBeingStep.Form -> ScreenId.WELL_BEING_FORM
        WellBeingStep.Validation -> ScreenId.WELL_BEING_VALIDATION
        WellBeingStep.Review -> ScreenId.WELL_BEING_REVIEW
        WellBeingStep.Success -> ScreenId.WELL_BEING_SUCCESS
    }
}
