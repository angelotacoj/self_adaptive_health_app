package com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                TaskProgressHeader("Paso 1 de 4", "Introducción", adaptiveUiState = state.adaptiveUiState)
                
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = "Esta es una simulación. No se registrará información médica real.",
                    isError = false
                )

                InstructionCard(
                    if (state.adaptiveUiState.isAdaptiveMode) "Instrucciones de la tarea" else "Dato asignado",
                    if (state.adaptiveUiState.isAdaptiveMode) {
                        listOf(
                            "Registre un valor ficticio.",
                            "Nivel de energía: 1 al 10.",
                            "Estado de ánimo simulado."
                        )
                    } else {
                        listOf("Registro de bienestar ficticio")
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                LargePrimaryButton("Iniciar formulario", { onAction(WellBeingAction.StartFormClicked) }, adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            WellBeingStep.Form -> {
                TaskProgressHeader("Paso 2 de 4", "Formulario de datos ficticios", adaptiveUiState = state.adaptiveUiState)
                
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = "Simulación: No ingrese datos reales.",
                    isError = false
                )

                OutlinedTextField(
                    value = state.energyLevel,
                    onValueChange = { onAction(WellBeingAction.EnergyLevelChanged(it)) },
                    label = { Text("Nivel de energía simulado (1 al 10)") },
                    supportingText = { Text(if (state.adaptiveUiState.contextualHelpVisible) "Ingrese un número del 1 al 10." else "Número del 1 al 10") },
                    textStyle = if (state.adaptiveUiState.isAdaptiveMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                    isError = state.errorMessage != null && state.errorMessage.contains("energía"),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Estado de ánimo simulado",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val moods = listOf("Tranquilo", "Cansado", "Animado", "Neutral")
                moods.forEach { option ->
                    com.angelotacoj.self_adaptive_health_app.core.ui.CheckableOptionRow(
                        label = option,
                        selected = state.mood == option,
                        onClick = { onAction(WellBeingAction.MoodSelected(option)) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.note,
                    onValueChange = { onAction(WellBeingAction.NoteChanged(it)) },
                    label = { Text("Observación (opcional)") },
                    textStyle = if (state.adaptiveUiState.isAdaptiveMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                if (state.errorMessage != null) {
                    com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                        message = state.errorMessage,
                        isError = true
                    )
                    if (state.adaptiveUiState.contextualHelpVisible || state.fieldErrorCount >= 2) Text("Por favor corrija los errores marcados.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
                ButtonRow(
                    primaryText = "Revisar registro",
                    onPrimary = {
                        val energy = state.energyLevel.toIntOrNull()
                        if (energy == null || energy !in 1..10 || state.mood.isBlank()) {
                            onLog(InteractionEventType.FIELD_ERROR, screenId, "Invalid fictitious value entered. Exact value not logged.")
                            onAdaptiveEvent(AdaptiveInteractionEventType.FIELD_ERROR, screenId)
                        }
                        onAction(WellBeingAction.ValidateAndReviewClicked)
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
            WellBeingStep.Review -> {
                TaskProgressHeader("Paso 3 de 4", "Revisión del registro simulado", adaptiveUiState = state.adaptiveUiState)
                SummaryReviewCard("Información ficticia", listOf(
                    "Energía (1-10)" to state.energyLevel,
                    "Estado de ánimo" to state.mood,
                    "Observación" to (state.note.takeIf { it.isNotBlank() } ?: "Ninguna"),
                    "Fecha simulada" to java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.forLanguageTag("es-ES")).format(java.util.Date())
                ), adaptiveUiState = state.adaptiveUiState)
                ButtonRow(
                    primaryText = "Guardar registro simulado",
                    onPrimary = {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Attempted to save simulated well-being record.")
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
                TaskProgressHeader("Paso 4 de 4", "Mensaje de éxito", adaptiveUiState = state.adaptiveUiState)
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
        WellBeingStep.Review -> ScreenId.WELL_BEING_REVIEW
        WellBeingStep.Success -> ScreenId.WELL_BEING_SUCCESS
    }
}
