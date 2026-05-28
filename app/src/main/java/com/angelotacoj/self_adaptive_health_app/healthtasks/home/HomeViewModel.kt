package com.angelotacoj.self_adaptive_health_app.healthtasks.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.core.logging.MapeKLog
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val session: ExperimentSessionState,
    val dataSet: FakeHealthDataSet
)

data class HomeUiState(
    val showCancelSessionConfirmation: Boolean = false,
    val showSessionHelp: Boolean = false
)

sealed interface HomeAction {
    data object AccessTaskClicked : HomeAction
    data object ConsultAppointmentClicked : HomeAction
    data object RegisterWellBeingClicked : HomeAction
    data object ConfigureReminderClicked : HomeAction
    data object ReviewInformationClicked : HomeAction
    data object ViewDebugLogsClicked : HomeAction
    data object HelpClicked : HomeAction
    data object DismissHelpClicked : HomeAction
    data object CancelSessionClicked : HomeAction
    data object DismissCancelSessionClicked : HomeAction
    data object ConfirmCancelSessionClicked : HomeAction
    data object ContinueToUeqClicked : HomeAction
}

sealed interface HomeEvent {
    data object OpenAccess : HomeEvent
    data object OpenAppointment : HomeEvent
    data object OpenWellBeing : HomeEvent
    data object OpenReminders : HomeEvent
    data object OpenSummary : HomeEvent
    data object OpenDebugLogs : HomeEvent
    data object HelpRequested : HomeEvent
    data object NavigateToSetup : HomeEvent
    data object OpenUeq : HomeEvent
}

class HomeViewModel : ViewModel() {
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAction(action: HomeAction) {
        if (action == HomeAction.CancelSessionClicked) {
            MapeKLog.nav("cancel session requested")
            _uiState.value = _uiState.value.copy(showCancelSessionConfirmation = true)
            return
        }
        if (action == HomeAction.DismissCancelSessionClicked) {
            _uiState.value = _uiState.value.copy(showCancelSessionConfirmation = false)
            return
        }
        if (action == HomeAction.DismissHelpClicked) {
            _uiState.value = _uiState.value.copy(showSessionHelp = false)
            return
        }
        val event = when (action) {
            HomeAction.AccessTaskClicked -> HomeEvent.OpenAccess
            HomeAction.ConsultAppointmentClicked -> HomeEvent.OpenAppointment
            HomeAction.RegisterWellBeingClicked -> HomeEvent.OpenWellBeing
            HomeAction.ConfigureReminderClicked -> HomeEvent.OpenReminders
            HomeAction.ReviewInformationClicked -> HomeEvent.OpenSummary
            HomeAction.ViewDebugLogsClicked -> HomeEvent.OpenDebugLogs
            HomeAction.ContinueToUeqClicked -> HomeEvent.OpenUeq
            HomeAction.HelpClicked -> {
                _uiState.value = _uiState.value.copy(showSessionHelp = true)
                HomeEvent.HelpRequested
            }
            HomeAction.ConfirmCancelSessionClicked -> {
                _uiState.value = _uiState.value.copy(showCancelSessionConfirmation = false)
                HomeEvent.NavigateToSetup
            }
            HomeAction.CancelSessionClicked,
            HomeAction.DismissCancelSessionClicked,
            HomeAction.DismissHelpClicked -> return
        }
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}
