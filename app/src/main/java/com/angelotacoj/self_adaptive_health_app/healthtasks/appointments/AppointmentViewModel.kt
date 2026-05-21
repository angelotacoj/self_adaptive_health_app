package com.angelotacoj.self_adaptive_health_app.healthtasks.appointments

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppointmentStep {
    Overview,
    List,
    Detail,
    Confirmation,
    Completed
}

data class AppointmentState(
    val step: AppointmentStep = AppointmentStep.Overview,
    val targetAppointment: Appointment,
    val appointmentOptions: List<Appointment>,
    val selectedAppointment: Appointment? = null,
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface AppointmentAction {
    data object StartListClicked : AppointmentAction
    data class AppointmentSelected(val appointment: Appointment) : AppointmentAction
    data object ContinueFromDetailClicked : AppointmentAction
    data object ConfirmFoundClicked : AppointmentAction
    data object ReviewAgainClicked : AppointmentAction
    data object BackClicked : AppointmentAction
    data object CancelClicked : AppointmentAction
    data class AdaptiveStateChanged(val adaptiveUiState: AdaptiveUiState) : AppointmentAction
}

sealed interface AppointmentEvent {
    data object ExitTask : AppointmentEvent
}

class AppointmentViewModel : ViewModel() {
    private val _state = MutableStateFlow<AppointmentState?>(null)
    val state: StateFlow<AppointmentState?> = _state.asStateFlow()

    fun start(target: Appointment, options: List<Appointment>) {
        if (_state.value?.targetAppointment != target) {
            _state.value = AppointmentState(targetAppointment = target, appointmentOptions = options)
        }
    }

    fun onAction(action: AppointmentAction): AppointmentEvent? {
        val current = _state.value ?: return null
        return when (action) {
            AppointmentAction.StartListClicked -> {
                _state.value = current.copy(step = AppointmentStep.List)
                null
            }
            is AppointmentAction.AppointmentSelected -> {
                _state.value = current.copy(step = AppointmentStep.Detail, selectedAppointment = action.appointment)
                null
            }
            AppointmentAction.ContinueFromDetailClicked -> {
                _state.value = current.copy(step = AppointmentStep.Confirmation)
                null
            }
            AppointmentAction.ConfirmFoundClicked -> {
                _state.value = current.copy(step = AppointmentStep.Completed)
                null
            }
            AppointmentAction.ReviewAgainClicked -> {
                _state.value = current.copy(step = AppointmentStep.Detail)
                null
            }
            AppointmentAction.BackClicked -> {
                when (current.step) {
                    AppointmentStep.Overview -> return AppointmentEvent.ExitTask
                    AppointmentStep.List -> _state.value = current.copy(step = AppointmentStep.Overview)
                    AppointmentStep.Detail -> _state.value = current.copy(step = AppointmentStep.List)
                    AppointmentStep.Confirmation -> _state.value = current.copy(step = AppointmentStep.Detail)
                    AppointmentStep.Completed -> return AppointmentEvent.ExitTask
                }
                null
            }
            AppointmentAction.CancelClicked -> AppointmentEvent.ExitTask
            is AppointmentAction.AdaptiveStateChanged -> {
                _state.value = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
