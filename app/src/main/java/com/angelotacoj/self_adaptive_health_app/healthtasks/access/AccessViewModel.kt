package com.angelotacoj.self_adaptive_health_app.healthtasks.access

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AccessStep {
    Intro,
    Code,
    Pin,
    Validation,
    Completed
}

enum class AccessErrorField {
    UserCode,
    SimulatedPin,
    Both
}

data class AccessState(
    val credentials: AccessCredentials,
    val step: AccessStep = AccessStep.Intro,
    val userCode: String = "",
    val simulatedPin: String = "",
    val errorMessage: String? = null,
    val errorField: AccessErrorField? = null,
    val showHelpDialog: Boolean = false,
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface AccessAction {
    data object StartClicked : AccessAction
    data class UserCodeChanged(val value: String) : AccessAction
    data class SimulatedPinChanged(val value: String) : AccessAction
    data object ContinueFromCodeClicked : AccessAction
    data object ValidateAccessClicked : AccessAction
    data object HelpClicked : AccessAction
    data object DismissHelpClicked : AccessAction
    data object AccessValidated : AccessAction
    data object BackClicked : AccessAction
    data object CancelClicked : AccessAction
    data class AdaptiveStateChanged(val adaptiveUiState: AdaptiveUiState) : AccessAction
}

sealed interface AccessEvent {
    data object ExitTask : AccessEvent
    data class FieldError(val fieldId: String, val errorType: String) : AccessEvent
}

class AccessViewModel : ViewModel() {
    private val _state = MutableStateFlow<AccessState?>(null)
    val state: StateFlow<AccessState?> = _state.asStateFlow()

    fun start(credentials: AccessCredentials) {
        if (_state.value?.credentials != credentials) {
            _state.value = AccessState(credentials = credentials)
        }
    }

    fun onAction(action: AccessAction): AccessEvent? {
        val current = _state.value ?: return null
        return when (action) {
            AccessAction.StartClicked -> {
                _state.value = current.copy(step = AccessStep.Code, errorMessage = null, errorField = null)
                null
            }

            is AccessAction.UserCodeChanged -> {
                _state.value = current.copy(userCode = action.value.lineSequence().firstOrNull().orEmpty().trim().uppercase(), errorMessage = null, errorField = null)
                null
            }

            is AccessAction.SimulatedPinChanged -> {
                _state.value = current.copy(simulatedPin = action.value.filter { it.isDigit() }.take(4), errorMessage = null, errorField = null)
                null
            }

            AccessAction.ContinueFromCodeClicked -> {
                validateCode(current)
            }

            AccessAction.ValidateAccessClicked -> {
                validateCredentials(current)
            }

            AccessAction.HelpClicked -> {
                _state.value = current.copy(showHelpDialog = true)
                null
            }

            AccessAction.DismissHelpClicked -> {
                _state.value = current.copy(showHelpDialog = false)
                null
            }

            AccessAction.AccessValidated -> {
                _state.value = current.copy(step = AccessStep.Validation, errorMessage = null, errorField = null)
                null
            }

            AccessAction.BackClicked -> {
                when (current.step) {
                    AccessStep.Intro -> return AccessEvent.ExitTask
                    AccessStep.Code -> _state.value = current.copy(step = AccessStep.Intro, errorMessage = null, errorField = null)
                    AccessStep.Pin -> _state.value = current.copy(step = AccessStep.Code, errorMessage = null, errorField = null)
                    AccessStep.Validation -> return AccessEvent.ExitTask
                    AccessStep.Completed -> return AccessEvent.ExitTask
                }
                null
            }

            AccessAction.CancelClicked -> AccessEvent.ExitTask

            is AccessAction.AdaptiveStateChanged -> {
                _state.value = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }

    fun finishTask() {
        _state.value = _state.value?.copy(step = AccessStep.Completed, errorMessage = null, errorField = null)
    }

    private fun validateCode(current: AccessState): AccessEvent? {
        val cleanCode = current.userCode.trim()
        return when {
            cleanCode.isBlank() -> {
                _state.value = current.copy(
                    step = AccessStep.Code,
                    errorMessage = "Ingrese el código de usuario mostrado en pantalla.",
                    errorField = AccessErrorField.UserCode
                )
                AccessEvent.FieldError(fieldId = "user_code", errorType = "empty")
            }

            cleanCode != current.credentials.userCode -> {
                _state.value = current.copy(
                    step = AccessStep.Code,
                    errorMessage = "El código o PIN no coincide con los datos simulados. Revise la información e inténtelo nuevamente.",
                    errorField = AccessErrorField.UserCode
                )
                AccessEvent.FieldError(fieldId = "user_code", errorType = "invalid")
            }

            else -> {
                _state.value = current.copy(step = AccessStep.Pin, errorMessage = null, errorField = null)
                null
            }
        }
    }

    private fun validateCredentials(current: AccessState): AccessEvent? {
        val cleanCode = current.userCode.trim()
        val cleanPin = current.simulatedPin.trim()
        return when {
            cleanCode.isBlank() -> {
                _state.value = current.copy(
                    step = AccessStep.Code,
                    errorMessage = "Ingrese el código de usuario mostrado en pantalla.",
                    errorField = AccessErrorField.UserCode
                )
                AccessEvent.FieldError(fieldId = "user_code", errorType = "empty")
            }

            cleanPin.isBlank() -> {
                _state.value = current.copy(
                    step = AccessStep.Pin,
                    errorMessage = "Ingrese el PIN simulado mostrado en pantalla.",
                    errorField = AccessErrorField.SimulatedPin
                )
                AccessEvent.FieldError(fieldId = "simulated_pin", errorType = "empty")
            }

            cleanCode != current.credentials.userCode || cleanPin != current.credentials.simulatedPin -> {
                _state.value = current.copy(
                    step = AccessStep.Pin,
                    errorMessage = "El código o PIN no coincide con los datos simulados. Revise la información e inténtelo nuevamente.",
                    errorField = AccessErrorField.Both
                )
                AccessEvent.FieldError(fieldId = "access_credentials", errorType = "invalid")
            }

            else -> {
                _state.value = current.copy(errorMessage = null, errorField = null)
                null
            }
        }
    }
}
