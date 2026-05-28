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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner
import java.text.SimpleDateFormat
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
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
            delay(AdaptiveTiming.getThresholdForScreen(screenId))
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
                TaskProgressHeader("Paso 1 de 5", "Introducción al recordatorio", adaptiveUiState = state.adaptiveUiState)
                
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = "Esta es una simulación. No se generarán notificaciones reales.",
                    isError = false
                )

                InstructionCard(
                    if (state.adaptiveUiState.isAdaptiveMode) "Instrucciones de la tarea" else "Recordatorio asignado",
                    if (state.adaptiveUiState.isAdaptiveMode) {
                        listOf("Configure un recordatorio para: ${state.activity}", "Hora: ${state.time}", "Frecuencia: ${state.frequency}")
                    } else {
                        listOf("${state.activity}, ${state.time}, ${state.frequency}")
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
                LargePrimaryButton("Iniciar configuración", { onAction(ReminderAction.StartNewReminderClicked) }, adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Necesito ayuda", { onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId) }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            ReminderStep.SelectType -> {
                TaskProgressHeader("Paso 2 de 5", "Seleccionar Tipo", adaptiveUiState = state.adaptiveUiState)
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = "Esta es una simulación. No se generarán notificaciones reales.",
                    isError = false
                )
                val options = listOf("Vitamina ficticia", "Actividad de autocuidado simulada", "Control simulado de bienestar", "Cita ficticia")
                options.forEach { option ->
                    com.angelotacoj.self_adaptive_health_app.core.ui.CheckableOptionRow(
                        label = option,
                        selected = state.selectedType == option,
                        onClick = { onAction(ReminderAction.TypeUpdated(option)) }
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                LargePrimaryButton(
                    text = "Continuar",
                    onClick = { onAction(ReminderAction.TypeNextClicked) },
                    enabled = state.selectedType.isNotBlank(),
                    adaptiveUiState = state.adaptiveUiState
                )
            }
            ReminderStep.SelectSchedule -> {
                TaskProgressHeader("Paso 3 de 5", "Horario y Frecuencia", adaptiveUiState = state.adaptiveUiState)
                NoticeBanner(
                    message = "Esta es una simulación. No se generarán notificaciones reales.",
                    isError = false
                )
                var showDatePicker by remember { mutableStateOf(false) }
                var showTimePicker by remember { mutableStateOf(false) }

                val DialogWhite = Color.White
                val PrimaryTeal = Color(0xFF00796B)
                val PrimaryTealDark = Color(0xFF00574B)
                val SoftTeal = Color(0xFFE0F2F1)

                val TextPrimary = Color(0xFF1F2937)
                val TextSecondary = Color(0xFF64748B)

                val SoftGray = Color(0xFFF1F5F9)
                val BorderGray = Color(0xFFE2E8F0)
                
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        colors = DatePickerDefaults.colors(containerColor = Color.White),
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = PrimaryTeal
                                ),
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val formatter = SimpleDateFormat(
                                            "dd 'de' MMMM",
                                            Locale.forLanguageTag("es-ES")
                                        ).apply {
                                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        }
                                        val date = formatter.format(java.util.Date(millis))
                                        onAction(ReminderAction.DateUpdated(date))
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("Aceptar", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                        }
                    ) {
                        DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = Color.White))
                    }
                }

                if (showTimePicker) {
                    val timePickerState = rememberTimePickerState()

                    AlertDialog(
                        containerColor = DialogWhite,
                        onDismissRequest = { showTimePicker = false },
                        confirmButton = {
                            TextButton(
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = PrimaryTeal
                                ),
                                onClick = {
                                    val cal = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                                    }

                                    val time = SimpleDateFormat(
                                        "h:mm a",
                                        Locale.forLanguageTag("es-ES")
                                    ).format(cal.time)

                                    onAction(ReminderAction.TimeUpdated(time))
                                    showTimePicker = false
                                }
                            ) {
                                Text("Aceptar", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = TextSecondary
                                ),
                                onClick = { showTimePicker = false }
                            ) {
                                Text("Cancelar")
                            }
                        },
                        text = {
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    containerColor = DialogWhite,
                                    clockDialColor = SoftGray,
                                    selectorColor = PrimaryTeal,
                                    clockDialSelectedContentColor = Color.White,
                                    clockDialUnselectedContentColor = TextPrimary,
                                    timeSelectorSelectedContainerColor = SoftTeal,
                                    timeSelectorSelectedContentColor = PrimaryTealDark,
                                    timeSelectorUnselectedContainerColor = SoftGray,
                                    timeSelectorUnselectedContentColor = TextPrimary,
                                    periodSelectorSelectedContainerColor = PrimaryTeal,
                                    periodSelectorSelectedContentColor = Color.White,
                                    periodSelectorUnselectedContainerColor = SoftGray,
                                    periodSelectorUnselectedContentColor = TextPrimary,
                                    periodSelectorBorderColor = BorderGray
                                )
                            )
                        }
                    )
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    onClick = { showDatePicker = true }
                ) {
                    androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp)) {
                        Text("Fecha seleccionada: ${state.selectedDate.takeIf { it.isNotEmpty() } ?: "Ninguna (tocar para elegir)"}")
                    }
                }
                
                androidx.compose.material3.OutlinedCard(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    onClick = { showTimePicker = true }
                ) {
                    androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                        androidx.compose.material3.Text("Hora seleccionada: ${state.selectedTime.takeIf { it.isNotEmpty() } ?: "Ninguna (tocar para elegir)"}")
                    }
                }
                val freqs = listOf("Una vez", "Diariamente", "Semanalmente")
                freqs.forEach { option ->
                    com.angelotacoj.self_adaptive_health_app.core.ui.CheckableOptionRow(
                        label = option,
                        selected = state.selectedFrequency == option,
                        onClick = { onAction(ReminderAction.FrequencyUpdated(option)) }
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                LargePrimaryButton(
                    text = "Continuar",
                    onClick = { onAction(ReminderAction.ScheduleNextClicked) },
                    enabled = state.selectedDate.isNotBlank() && state.selectedTime.isNotBlank() && state.selectedFrequency.isNotBlank(),
                    adaptiveUiState = state.adaptiveUiState
                )
            }
            ReminderStep.SelectDetails -> {
                TaskProgressHeader("Paso 4 de 5", "Detalles Opcionales", adaptiveUiState = state.adaptiveUiState)
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = "Esta es una simulación. No se generarán notificaciones reales.",
                    isError = false
                )
                androidx.compose.material3.OutlinedTextField(
                    value = state.optionalLocation,
                    onValueChange = { onAction(ReminderAction.LocationUpdated(it)) },
                    label = { androidx.compose.material3.Text("Ubicación ficticia (Opcional)") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = state.optionalNote,
                    onValueChange = { onAction(ReminderAction.NoteUpdated(it)) },
                    label = { androidx.compose.material3.Text("Nota simulada (Opcional)") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                LargePrimaryButton(
                    text = "Revisar configuración",
                    onClick = { onAction(ReminderAction.DetailsNextClicked) },
                    adaptiveUiState = state.adaptiveUiState
                )
            }
            ReminderStep.ReviewSummary -> {
                TaskProgressHeader("Paso 5 de 5", "Revisar recordatorio", adaptiveUiState = state.adaptiveUiState)
                com.angelotacoj.self_adaptive_health_app.core.ui.NoticeBanner(
                    message = "Esta es una simulación. No se generarán notificaciones reales.",
                    isError = false
                )
                SummaryReviewCard("Resumen del recordatorio simulado", listOf(
                    "Tipo" to state.selectedType,
                    "Fecha" to state.selectedDate,
                    "Hora" to state.selectedTime,
                    "Frecuencia" to state.selectedFrequency,
                    "Ubicación" to (state.optionalLocation.takeIf { it.isNotBlank() } ?: "No especificada"),
                    "Nota" to (state.optionalNote.takeIf { it.isNotBlank() } ?: "No especificada")
                ), adaptiveUiState = state.adaptiveUiState)
                
                ButtonRow(
                    primaryText = "Guardar simulación",
                    onPrimary = {
                        onLog(InteractionEventType.SENSITIVE_ACTION, screenId, "Attempted to save simulated reminder.")
                        val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId)
                        if (!requiresValidation) {
                            onAction(ReminderAction.SaveReminderClicked)
                            onLog(InteractionEventType.TASK_COMPLETED, ScreenId.REMINDER_SAVED, "T4 completed.")
                        }
                    },
                    secondaryText = "Volver y editar",
                    onSecondary = { onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId); onAction(ReminderAction.BackClicked) },
                    adaptiveUiState = state.adaptiveUiState
                )
                if (state.adaptiveUiState.safeExitEnabled || !state.adaptiveUiState.isAdaptiveMode) {
                    LargeSecondaryButton("Cancelar", { if (onAction(ReminderAction.CancelClicked) is ReminderEvent.ExitTask) onExit() }, adaptiveUiState = state.adaptiveUiState)
                }
            }
            ReminderStep.Saved -> {
                TaskProgressHeader("Completado", "Simulación guardada", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Recordatorio guardado", listOf("${state.selectedType} a las ${state.selectedTime}", state.selectedFrequency), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }
    }
}

private fun ReminderStep.toScreenId(): ScreenId {
    return when (this) {
        ReminderStep.Intro -> ScreenId.REMINDER_INTRO
        ReminderStep.SelectType -> ScreenId.REMINDER_ACTIVITY
        ReminderStep.SelectSchedule -> ScreenId.REMINDER_TIME
        ReminderStep.SelectDetails -> ScreenId.REMINDER_FREQUENCY
        ReminderStep.ReviewSummary -> ScreenId.REMINDER_REVIEW
        ReminderStep.Saved -> ScreenId.REMINDER_SAVED
    }
}
