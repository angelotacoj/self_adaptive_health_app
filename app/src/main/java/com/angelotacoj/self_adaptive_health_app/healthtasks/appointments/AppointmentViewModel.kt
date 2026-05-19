package com.angelotacoj.self_adaptive_health_app.healthtasks.appointments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.Appointment

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
    var state: AppointmentState? by mutableStateOf(null)
        private set

    fun start(target: Appointment, options: List<Appointment>) {
        if (state?.targetAppointment != target) {
            state = AppointmentState(targetAppointment = target, appointmentOptions = options)
        }
    }

    fun onAction(action: AppointmentAction): AppointmentEvent? {
        val current = state ?: return null
        return when (action) {
            AppointmentAction.StartListClicked -> {
                state = current.copy(step = AppointmentStep.List)
                null
            }
            is AppointmentAction.AppointmentSelected -> {
                state = current.copy(step = AppointmentStep.Detail, selectedAppointment = action.appointment)
                null
            }
            AppointmentAction.ContinueFromDetailClicked -> {
                state = current.copy(step = AppointmentStep.Confirmation)
                null
            }
            AppointmentAction.ConfirmFoundClicked -> {
                state = current.copy(step = AppointmentStep.Completed)
                null
            }
            AppointmentAction.ReviewAgainClicked -> {
                state = current.copy(step = AppointmentStep.Detail)
                null
            }
            AppointmentAction.BackClicked -> {
                when (current.step) {
                    AppointmentStep.Overview -> AppointmentEvent.ExitTask
                    AppointmentStep.List -> state = current.copy(step = AppointmentStep.Overview)
                    AppointmentStep.Detail -> state = current.copy(step = AppointmentStep.List)
                    AppointmentStep.Confirmation -> state = current.copy(step = AppointmentStep.Detail)
                    AppointmentStep.Completed -> return AppointmentEvent.ExitTask
                }
                null
            }
            AppointmentAction.CancelClicked -> AppointmentEvent.ExitTask
            is AppointmentAction.AdaptiveStateChanged -> {
                state = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
