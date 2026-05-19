package com.angelotacoj.self_adaptive_health_app.healthtasks.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class HomeState(
    val session: ExperimentSessionState,
    val dataSet: FakeHealthDataSet
)

sealed interface HomeAction {
    data object ConsultAppointmentClicked : HomeAction
    data object RegisterWellBeingClicked : HomeAction
    data object ConfigureReminderClicked : HomeAction
    data object ReviewInformationClicked : HomeAction
    data object ViewDebugLogsClicked : HomeAction
    data object HelpClicked : HomeAction
    data object CancelSessionClicked : HomeAction
}

sealed interface HomeEvent {
    data object OpenAppointments : HomeEvent
    data object OpenWellBeing : HomeEvent
    data object OpenReminders : HomeEvent
    data object OpenSummary : HomeEvent
    data object OpenDebugLogs : HomeEvent
    data object HelpRequested : HomeEvent
    data object NavigateToSetup : HomeEvent
}

class HomeViewModel : ViewModel() {
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun onAction(action: HomeAction) {
        val event = when (action) {
            HomeAction.ConsultAppointmentClicked -> HomeEvent.OpenAppointments
            HomeAction.RegisterWellBeingClicked -> HomeEvent.OpenWellBeing
            HomeAction.ConfigureReminderClicked -> HomeEvent.OpenReminders
            HomeAction.ReviewInformationClicked -> HomeEvent.OpenSummary
            HomeAction.ViewDebugLogsClicked -> HomeEvent.OpenDebugLogs
            HomeAction.HelpClicked -> HomeEvent.HelpRequested
            HomeAction.CancelSessionClicked -> HomeEvent.NavigateToSetup
        }
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}
