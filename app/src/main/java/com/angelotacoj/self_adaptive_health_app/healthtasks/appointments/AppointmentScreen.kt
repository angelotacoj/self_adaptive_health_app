package com.angelotacoj.self_adaptive_health_app.healthtasks.appointments

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
    onKeepAdaptation: () -> Unit,
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
        if (AdaptiveTiming.prolongedTimeDetectionEnabled) {
            delay(AdaptiveTiming.getThresholdForScreen(screenId))
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
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onKeepAdaptation, state.adaptiveUiState)

        when (state.step) {
            AppointmentStep.Overview -> {
                TaskProgressHeader("Paso 1 de 4", "Resumen de citas", adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    InstructionCard(
                        "Instrucciones de la tarea",
                        listOf(
                            "Verá tres citas ficticias.",
                            "Encuentre la cita asignada a este participante.",
                            "Recuerde la fecha, la hora y la indicación principal."
                        ),
                        adaptiveUiState = state.adaptiveUiState
                    )
                }
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
                TaskProgressHeader("Paso 2 de 4", "Lista de citas", adaptiveUiState = state.adaptiveUiState)
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
                TaskProgressHeader("Paso 3 de 4", "Detalle de la cita", adaptiveUiState = state.adaptiveUiState)
                
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = appointment.simulationDisclaimer,
                    isError = false
                )

                SummaryReviewCard(
                    title = "Información del Especialista",
                    rows = listOf(
                        "Profesional" to appointment.professionalName,
                        "Especialidad" to appointment.specialty
                    ),
                    adaptiveUiState = state.adaptiveUiState
                )
                
                SummaryReviewCard(
                    title = "Horario y Ubicación",
                    rows = listOf(
                        "Fecha" to appointment.date,
                        "Hora" to appointment.time,
                        "Ubicación" to appointment.location,
                        "Accesibilidad" to appointment.accessibilityNote
                    ),
                    adaptiveUiState = state.adaptiveUiState
                )
                
                SummaryReviewCard(
                    title = "Instrucciones",
                    rows = listOf(
                        "Preparación" to appointment.preparation,
                        "Traer" to appointment.itemsToBring,
                        "Indicación Principal" to appointment.instruction
                    ),
                    adaptiveUiState = state.adaptiveUiState
                )
                ButtonRow(
                    primaryText = "Confirmar",
                    onPrimary = {
                        onLog(InteractionEventType.BUTTON_CLICKED, screenId, "Appointment detail reviewed.")
                        onAction(AppointmentAction.ContinueFromDetailClicked)
                    },
                    secondaryText = "Volver a la lista",
                    onSecondary = {
                        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
                        onAction(AppointmentAction.BackClicked)
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            AppointmentStep.Confirmation -> {
                TaskProgressHeader("Paso 4 de 4", "Pregunta de confirmación", adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.contextualHelpVisible || state.adaptiveUiState.guidedModeEnabled) {
                    InstructionCard("¿Identificó la fecha, la hora y la indicación principal?", listOf("Esta es una consulta simulada. Si necesita volver a leer, elija 'Revisar nuevamente'."), adaptiveUiState = state.adaptiveUiState)
                }
                ButtonRow(
                    primaryText = "Completar la tarea",
                    onPrimary = {
                        onLog(InteractionEventType.TASK_COMPLETED, screenId, "T2 completed.")
                        onAction(AppointmentAction.ConfirmFoundClicked)
                        onTaskCompleted()
                    },
                    secondaryText = "Revisar nuevamente",
                    onSecondary = {
                        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
                        onAction(AppointmentAction.ReviewAgainClicked)
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
            }
            AppointmentStep.Completed -> {
                TaskProgressHeader("Completado", "Tarea completada", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Cita revisada", listOf("La información ficticia de la cita fue encontrada y confirmada."), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }
        if (state.adaptiveUiState.safeExitEnabled || !state.adaptiveUiState.isAdaptiveMode) {
            LargeSecondaryButton("Cancelar tarea", { onLog(InteractionEventType.BUTTON_CLICKED, screenId, "T2 cancelled."); onExit() }, adaptiveUiState = state.adaptiveUiState)
        }
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
        rows = listOf("Fecha" to appointment.date, "Hora" to appointment.time),
        adaptiveUiState = adaptiveUiState
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
