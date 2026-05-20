package com.angelotacoj.self_adaptive_health_app.healthtasks.appointments

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
import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import com.angelotacoj.self_adaptive_health_app.core.ui.ButtonRow
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.SummaryReviewCard
import com.angelotacoj.self_adaptive_health_app.core.ui.TaskProgressHeader

@Composable
fun AppointmentScreen(
    state: AppointmentState,
    onAction: (AppointmentAction) -> AppointmentEvent?,
    onLog: (InteractionEventType, ScreenId, String) -> Unit,
    onAdaptiveEvent: (AdaptiveInteractionEventType, ScreenId) -> Boolean,
    onApplyAdaptation: () -> Unit,
    onRejectAdaptation: () -> Unit,
    onUndoAdaptation: () -> Unit,
    onHideHelp: () -> Unit,
    onTaskCompleted: () -> Unit,
    onExit: () -> Unit
) {
    val screenId = state.step.toScreenId()
    BackHandler {
        onLog(InteractionEventType.BACK_PRESSED, screenId, "Device back pressed in appointment task.")
        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
        if (onAction(AppointmentAction.BackClicked) is AppointmentEvent.ExitTask) onExit()
    }
    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "Appointment step entered: $screenId.")
        if (state.adaptiveUiState.isAdaptiveMode) {
            delay(90_000)
            onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, screenId)
        }
    }

    ScreenContainer(
        title = "Cita médica",
        subtitle = "Encuentre la fecha, hora e indicación de la cita asignada.",
        navigationLabel = "Volver",
        onNavigationClick = {
            onLog(InteractionEventType.BACK_PRESSED, screenId, "Back pressed in appointment task.")
            onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
            if (onAction(AppointmentAction.BackClicked) is AppointmentEvent.ExitTask) onExit()
        },
        adaptiveUiState = state.adaptiveUiState
    ) {
        AdaptiveSuggestionCard(state.adaptiveUiState.pendingAdaptation, onApplyAdaptation, onRejectAdaptation, state.adaptiveUiState)
        AdaptiveConfirmationDialog(state.adaptiveUiState.pendingAdaptation, onApplyAdaptation, onRejectAdaptation, onRejectAdaptation, state.adaptiveUiState)
        ContextualHelpBox(state.adaptiveUiState, onHideHelp)
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onHideHelp, state.adaptiveUiState)

        when (state.step) {
            AppointmentStep.Overview -> {
                TaskProgressHeader("Paso 1 de 4", "Resumen de citas")
                InstructionCard(
                    "Instrucciones de la tarea",
                    listOf(
                        "Verá tres citas ficticias.",
                        "Encuentre la cita asignada a este participante.",
                        "Recuerde la fecha, la hora y la indicación principal."
                    )
                )
                LargePrimaryButton(
                    text = "Ver lista de citas",
                    onClick = {
                        onLog(InteractionEventType.BUTTON_CLICKED, screenId, "Appointment overview continued.")
                        onAction(AppointmentAction.StartListClicked)
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            AppointmentStep.List -> {
                TaskProgressHeader("Paso 2 de 4", "Lista de citas")
                state.appointmentOptions.forEach { appointment ->
                    AppointmentOptionCard(
                        appointment = appointment,
                        adaptiveUiState = state.adaptiveUiState,
                        onClick = {
                            onLog(InteractionEventType.BUTTON_CLICKED, screenId, "Appointment selected: ${appointment.title}.")
                            onAction(AppointmentAction.AppointmentSelected(appointment))
                        }
                    )
                }
            }
            AppointmentStep.Detail -> {
                val appointment = state.selectedAppointment ?: state.targetAppointment
                TaskProgressHeader("Paso 3 de 4", "Detalle de la cita")
                SummaryReviewCard(
                    title = appointment.title,
                    rows = listOf(
                        "Fecha" to appointment.date,
                        "Hora" to appointment.time,
                        "Indicación" to appointment.instruction
                    )
                )
                ButtonRow(
                    primaryText = "Continuar a confirmación",
                    onPrimary = {
                        onLog(InteractionEventType.BUTTON_CLICKED, screenId, "Appointment detail reviewed.")
                        onAction(AppointmentAction.ContinueFromDetailClicked)
                    },
                    secondaryText = "Volver a la lista",
                    onSecondary = {
                        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
                        onAction(AppointmentAction.BackClicked)
                    }
                )
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            AppointmentStep.Confirmation -> {
                TaskProgressHeader("Paso 4 de 4", "Pregunta de confirmación")
                InstructionCard("¿Encontró la fecha, la hora y la indicación?", listOf("Si no está seguro, puede revisar el detalle nuevamente."))
                ButtonRow(
                    primaryText = "Sí, continuar",
                    onPrimary = {
                        onLog(InteractionEventType.TASK_COMPLETED, screenId, "T2 completed.")
                        onAction(AppointmentAction.ConfirmFoundClicked)
                        onTaskCompleted()
                    },
                    secondaryText = "Revisar nuevamente",
                    onSecondary = {
                        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
                        onAction(AppointmentAction.ReviewAgainClicked)
                    }
                )
            }
            AppointmentStep.Completed -> {
                TaskProgressHeader("Completado", "Tarea completada")
                InstructionCard("Cita revisada", listOf("La información ficticia de la cita fue encontrada y confirmada."))
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }
        LargeSecondaryButton("Cancelar tarea", { onLog(InteractionEventType.BUTTON_CLICKED, screenId, "T1 cancelled."); onExit() }, adaptiveUiState = state.adaptiveUiState)
    }
}

@Composable
private fun AppointmentOptionCard(
    appointment: Appointment,
    adaptiveUiState: com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState,
    onClick: () -> Unit
) {
    SummaryReviewCard(
        title = appointment.title,
        rows = listOf("Fecha" to appointment.date, "Hora" to appointment.time, "Indicación" to appointment.instruction)
    )
    LargePrimaryButton("Abrir ${appointment.title}", onClick, adaptiveUiState = adaptiveUiState)
}

private fun AppointmentStep.toScreenId(): ScreenId {
    return when (this) {
        AppointmentStep.Overview -> ScreenId.APPOINTMENT_OVERVIEW
        AppointmentStep.List -> ScreenId.APPOINTMENT_LIST
        AppointmentStep.Detail -> ScreenId.APPOINTMENT_DETAIL
        AppointmentStep.Confirmation -> ScreenId.APPOINTMENT_CONFIRMATION
        AppointmentStep.Completed -> ScreenId.APPOINTMENT_COMPLETED
    }
}
