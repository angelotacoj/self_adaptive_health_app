package com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
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
    onExit: () -> Unit
) {
    val screenId = state.step.toScreenId()
    BackHandler {
        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
        if (onAction(WellBeingAction.BackClicked) is WellBeingEvent.ExitTask) onExit()
    }
    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "Well-being step entered: $screenId.")
        delay(120_000)
        onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, screenId)
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
                    onLog(InteractionEventType.TASK_COMPLETED, ScreenId.WELL_BEING_SUCCESS, "T2 completed.")
                }
            },
            onEdit = { onAction(WellBeingAction.EditClicked) },
            onCancel = onRejectAdaptation,
            adaptiveUiState = state.adaptiveUiState
        )
        ContextualHelpBox(state.adaptiveUiState, onHideHelp)
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onHideHelp, state.adaptiveUiState)

        when (state.step) {
            WellBeingStep.Intro -> {
                TaskProgressHeader("Paso 1 de 5", "Introducción")
                InstructionCard(
                    "Instrucciones de la tarea",
                    listOf(
                        "Use el valor ficticio asignado.",
                        "${state.label}: ${state.suggestedValue}",
                        "Rango aceptado: 1 a 10."
                    )
                )
                LargePrimaryButton("Iniciar formulario", { onAction(WellBeingAction.StartFormClicked) }, adaptiveUiState = state.adaptiveUiState)
                LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
            }
            WellBeingStep.Form -> {
                TaskProgressHeader("Paso 2 de 5", "Formulario de datos ficticios")
                OutlinedTextField(
                    value = state.valueText,
                    onValueChange = { onAction(WellBeingAction.ValueChanged(it)) },
                    label = { Text(state.label) },
                    supportingText = { Text("Ingrese un número del 1 al 10. Ejemplo: ${state.suggestedValue}") },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    isError = state.errorMessage != null
                )
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    if (state.fieldErrorCount >= 2) Text("Ejemplo: ingrese ${state.suggestedValue}.", style = MaterialTheme.typography.bodyLarge)
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
                    secondaryText = "Cancelar",
                    onSecondary = { if (onAction(WellBeingAction.CancelClicked) is WellBeingEvent.ExitTask) onExit() }
                )
            }
            WellBeingStep.Validation -> {
                TaskProgressHeader("Paso 3 de 5", "Validación")
                InstructionCard("Valor aceptado", listOf("El valor ficticio está dentro del rango aceptado.", "Continúe para revisar antes de guardar."))
                LargePrimaryButton("Revisar antes de guardar", { onAction(WellBeingAction.ContinueToReviewClicked) }, adaptiveUiState = state.adaptiveUiState)
            }
            WellBeingStep.Review -> {
                TaskProgressHeader("Paso 4 de 5", "Revisión antes de guardar")
                SummaryReviewCard("Esta información es simulada", listOf(state.label to state.valueText))
                ButtonRow(
                    primaryText = "Guardar",
                    onPrimary = {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Attempted to save simulated well-being record. Exact value not logged.")
                        val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId)
                        if (!requiresValidation) {
                            onAction(WellBeingAction.SaveClicked)
                            onLog(InteractionEventType.TASK_COMPLETED, ScreenId.WELL_BEING_SUCCESS, "T2 completed.")
                        }
                    },
                    secondaryText = "Editar",
                    onSecondary = { onAction(WellBeingAction.EditClicked) }
                )
                LargeSecondaryButton("Cancelar", { if (onAction(WellBeingAction.CancelClicked) is WellBeingEvent.ExitTask) onExit() }, adaptiveUiState = state.adaptiveUiState)
            }
            WellBeingStep.Success -> {
                TaskProgressHeader("Paso 5 de 5", "Mensaje de éxito")
                InstructionCard("Registro ficticio guardado", listOf("Este valor simulado no fue almacenado como dato clínico."))
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
