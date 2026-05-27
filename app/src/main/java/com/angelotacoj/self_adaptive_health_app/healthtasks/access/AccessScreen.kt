package com.angelotacoj.self_adaptive_health_app.healthtasks.access

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun AccessScreen(
    state: AccessState,
    onAction: (AccessAction) -> AccessEvent?,
    onLog: (InteractionEventType, ScreenId, String) -> Unit,
    onFieldError: (ScreenId, String, String) -> Unit,
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
    val codeFocusRequester = remember { FocusRequester() }
    val pinFocusRequester = remember { FocusRequester() }

    BackHandler {
        onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
        if (onAction(AccessAction.BackClicked) is AccessEvent.ExitTask) onExit()
    }

    LaunchedEffect(screenId) {
        onLog(InteractionEventType.SCREEN_ENTERED, screenId, "Access step entered: $screenId.")
        if (AdaptiveTiming.prolongedTimeDetectionEnabled) {
            delay(AdaptiveTiming.getThresholdForScreen(screenId))
            onAdaptiveEvent(AdaptiveInteractionEventType.PROLONGED_TIME, screenId)
        }
    }

    LaunchedEffect(state.errorField, state.step) {
        when (state.errorField) {
            AccessErrorField.UserCode -> codeFocusRequester.requestFocus()
            AccessErrorField.SimulatedPin,
            AccessErrorField.Both -> pinFocusRequester.requestFocus()
            null -> Unit
        }
    }

    if (state.adaptiveUiState.isAdaptiveMode && state.showHelpDialog) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { onAction(AccessAction.DismissHelpClicked) },
            title = { Text("Ayuda para acceder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use exactamente el código ficticio que aparece en la tarjeta de credenciales.", style = adaptiveBodyStyle(state))
                    Text("En este paso escriba el código de usuario. Luego presione Continuar para ingresar el PIN simulado.", style = adaptiveBodyStyle(state))
                    Text("No ingrese datos personales reales.", style = adaptiveBodyStyle(state))
                }
            },
            confirmButton = {
                LargeSecondaryButton("Entendido", { onAction(AccessAction.DismissHelpClicked) }, adaptiveUiState = state.adaptiveUiState)
            }
        )
    }

    ScreenContainer(
        title = "Acceder con código/PIN simulado",
        subtitle = "Use datos ficticios para acceder a la aplicación.",
        navigationLabel = "Volver",
        onNavigationClick = {
            onAdaptiveEvent(AdaptiveInteractionEventType.BACK_PRESSED, screenId)
            if (onAction(AccessAction.BackClicked) is AccessEvent.ExitTask) onExit()
        },
        adaptiveUiState = state.adaptiveUiState
    ) {
        AdaptiveSuggestionCard(state.adaptiveUiState.pendingAdaptation, onApplyAdaptation, onRejectAdaptation, state.adaptiveUiState)
        AdaptiveConfirmationDialog(
            pending = state.adaptiveUiState.pendingAdaptation,
            onConfirm = {
                onApplyAdaptation()
                onAction(AccessAction.AccessValidated)
            },
            onEdit = onRejectAdaptation,
            onCancel = onRejectAdaptation,
            adaptiveUiState = state.adaptiveUiState
        )
        ContextualHelpBox(state.adaptiveUiState, onHideHelp)
        UndoAdaptationCard(state.adaptiveUiState.undoMessageVisible, onUndoAdaptation, onKeepAdaptation, state.adaptiveUiState)

        when (state.step) {
            AccessStep.Intro -> {
                TaskProgressHeader("Paso 1 de 5", "Acceso simulado", adaptiveUiState = state.adaptiveUiState)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    InstructionCard(
                        "Instrucciones de la tarea",
                        listOf(
                            "Los datos son simulados. No ingrese información personal real.",
                            "Para esta tarea, use las credenciales ficticias mostradas en pantalla."
                        ),
                        adaptiveUiState = state.adaptiveUiState
                    )
                }
                CredentialCard(state)
                LargePrimaryButton("Comenzar", { onAction(AccessAction.StartClicked) }, adaptiveUiState = state.adaptiveUiState)
            }

            AccessStep.Code -> {
                TaskProgressHeader("Paso 2 de 5", "Código de usuario", adaptiveUiState = state.adaptiveUiState)
                CredentialCard(state)
                OutlinedTextField(
                    value = state.userCode,
                    onValueChange = { onAction(AccessAction.UserCodeChanged(it)) },
                    label = { Text("Código de usuario", style = adaptiveLabelStyle(state)) },
                    supportingText = { Text(if (state.adaptiveUiState.contextualHelpVisible) "Use el código ficticio mostrado en pantalla." else "Código asignado", style = adaptiveBodyStyle(state)) },
                    isError = state.errorField == AccessErrorField.UserCode,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            val event = onAction(AccessAction.ContinueFromCodeClicked)
                            if (event is AccessEvent.FieldError) {
                                handleFieldEvent(event, screenId, onFieldError, onAdaptiveEvent)
                            } else {
                                pinFocusRequester.requestFocus()
                            }
                        }
                    ),
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .focusRequester(codeFocusRequester)
                )
                ErrorText(state)
                if (state.adaptiveUiState.isAdaptiveMode) {
                    ButtonRow(
                        primaryText = "Continuar",
                        onPrimary = {
                            handleFieldEvent(onAction(AccessAction.ContinueFromCodeClicked), screenId, onFieldError, onAdaptiveEvent)
                        },
                        secondaryText = "Necesito ayuda",
                        onSecondary = {
                            onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId)
                            onAction(AccessAction.HelpClicked)
                        },
                        adaptiveUiState = state.adaptiveUiState
                    )
                } else {
                    LargePrimaryButton(
                        "Continuar",
                        { handleFieldEvent(onAction(AccessAction.ContinueFromCodeClicked), screenId, onFieldError, onAdaptiveEvent) },
                        adaptiveUiState = state.adaptiveUiState
                    )
                }
            }

            AccessStep.Pin -> {
                TaskProgressHeader("Paso 3 de 5", "PIN simulado", adaptiveUiState = state.adaptiveUiState)
                CredentialCard(state)
                OutlinedTextField(
                    value = state.simulatedPin,
                    onValueChange = { onAction(AccessAction.SimulatedPinChanged(it)) },
                    label = { Text("PIN simulado", style = adaptiveLabelStyle(state)) },
                    supportingText = { Text(if (state.adaptiveUiState.contextualHelpVisible) "Use el PIN simulado mostrado en pantalla." else "PIN asignado", style = adaptiveBodyStyle(state)) },
                    isError = state.errorField == AccessErrorField.SimulatedPin || state.errorField == AccessErrorField.Both,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val event = onAction(AccessAction.ValidateAccessClicked)
                            if (event is AccessEvent.FieldError) {
                                handleFieldEvent(event, screenId, onFieldError, onAdaptiveEvent)
                            } else {
                                val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId)
                                if (!requiresValidation) onAction(AccessAction.AccessValidated)
                            }
                        }
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .focusRequester(pinFocusRequester)
                )
                ErrorText(state)
                val validateAccess: () -> Unit = {
                    val event = onAction(AccessAction.ValidateAccessClicked)
                    if (event is AccessEvent.FieldError) {
                        handleFieldEvent(event, screenId, onFieldError, onAdaptiveEvent)
                    } else {
                        val requiresValidation = onAdaptiveEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION, screenId)
                        if (!requiresValidation) onAction(AccessAction.AccessValidated)
                    }
                }
                if (state.adaptiveUiState.isAdaptiveMode) {
                    ButtonRow(
                        primaryText = "Validar acceso",
                        onPrimary = validateAccess,
                        secondaryText = "Necesito ayuda",
                        onSecondary = {
                            onAdaptiveEvent(AdaptiveInteractionEventType.HELP_REQUESTED, screenId)
                            onAction(AccessAction.HelpClicked)
                        },
                        adaptiveUiState = state.adaptiveUiState
                    )
                } else {
                    LargePrimaryButton("Validar acceso", validateAccess, adaptiveUiState = state.adaptiveUiState)
                }
            }

            AccessStep.Validation -> {
                TaskProgressHeader("Paso 4 de 5", "Validación", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Acceso concedido", listOf("Ha completado la tarea de acceso simulado."), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton(
                    "Finalizar tarea",
                    {
                        onAction(AccessAction.AccessValidated)
                        onTaskCompleted()
                    },
                    adaptiveUiState = state.adaptiveUiState
                )
            }

            AccessStep.Completed -> {
                TaskProgressHeader("Paso 5 de 5", "Tarea completada", adaptiveUiState = state.adaptiveUiState)
                InstructionCard("Tarea completada", listOf("Ha completado la tarea de acceso simulado."), adaptiveUiState = state.adaptiveUiState)
                LargePrimaryButton("Volver al inicio", onExit, adaptiveUiState = state.adaptiveUiState)
            }
        }

        if (state.step != AccessStep.Completed) {
            if (state.adaptiveUiState.safeExitEnabled || !state.adaptiveUiState.isAdaptiveMode) {
                LargeSecondaryButton(
                    "Cancelar tarea",
                    { if (onAction(AccessAction.CancelClicked) is AccessEvent.ExitTask) onExit() },
                    adaptiveUiState = state.adaptiveUiState
                )
            }
        }
    }
}

@Composable
private fun CredentialCard(state: AccessState) {
    SummaryReviewCard(
        title = "Credenciales ficticias",
        rows = listOf(
            "Código de usuario" to state.credentials.userCode,
            "PIN simulado" to state.credentials.simulatedPin
        ),
        adaptiveUiState = state.adaptiveUiState
    )
}

@Composable
private fun ErrorText(state: AccessState) {
    if (state.errorMessage != null) {
        Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = adaptiveBodyStyle(state))
    }
}

@Composable
private fun adaptiveBodyStyle(state: AccessState) =
    MaterialTheme.typography.bodyLarge.copy(fontSize = ((if (state.adaptiveUiState.isAdaptiveMode) 17 else 14) * state.adaptiveUiState.textScale).sp)

@Composable
private fun adaptiveLabelStyle(state: AccessState) =
    MaterialTheme.typography.titleMedium.copy(fontSize = ((if (state.adaptiveUiState.isAdaptiveMode) 17 else 14) * state.adaptiveUiState.textScale).sp)

private fun handleFieldEvent(
    event: AccessEvent?,
    screenId: ScreenId,
    onFieldError: (ScreenId, String, String) -> Unit,
    onAdaptiveEvent: (AdaptiveInteractionEventType, ScreenId) -> Boolean
) {
    if (event is AccessEvent.FieldError) {
        onFieldError(screenId, event.fieldId, event.errorType)
        onAdaptiveEvent(AdaptiveInteractionEventType.FIELD_ERROR, screenId)
    }
}

private fun AccessStep.toScreenId(): ScreenId {
    return when (this) {
        AccessStep.Intro -> ScreenId.ACCESS_INTRO
        AccessStep.Code -> ScreenId.ACCESS_CODE
        AccessStep.Pin -> ScreenId.ACCESS_PIN
        AccessStep.Validation -> ScreenId.ACCESS_VALIDATION
        AccessStep.Completed -> ScreenId.ACCESS_COMPLETED
    }
}
