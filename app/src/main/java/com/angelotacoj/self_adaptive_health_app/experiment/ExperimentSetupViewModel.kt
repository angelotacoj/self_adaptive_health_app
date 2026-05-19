package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExperimentSetupState(
    val participantCode: String = "",
    val selectedGroup: ExperimentGroup = ExperimentGroup.GroupA,
    val errorMessage: String? = null
) {
    val selectedOrder: String = selectedGroup.orderDescription
}

sealed interface ExperimentSetupAction {
    data class ParticipantCodeChanged(val value: String) : ExperimentSetupAction
    data class GroupSelected(val group: ExperimentGroup) : ExperimentSetupAction
    data object StartSessionClicked : ExperimentSetupAction
}

sealed interface ExperimentSetupEvent {
    data class StartSession(val session: ExperimentSession) : ExperimentSetupEvent
}

class ExperimentSetupViewModel : ViewModel() {
    private val _state = MutableStateFlow(ExperimentSetupState())
    val state: StateFlow<ExperimentSetupState> = _state.asStateFlow()

    fun onAction(action: ExperimentSetupAction): ExperimentSetupEvent? {
        return when (action) {
            is ExperimentSetupAction.ParticipantCodeChanged -> {
                _state.value = _state.value.copy(participantCode = action.value.uppercase(), errorMessage = null)
                null
            }

            is ExperimentSetupAction.GroupSelected -> {
                _state.value = _state.value.copy(selectedGroup = action.group, errorMessage = null)
                null
            }

            ExperimentSetupAction.StartSessionClicked -> {
                val cleanCode = _state.value.participantCode.trim()
                if (cleanCode.isBlank()) {
                    _state.value = _state.value.copy(errorMessage = "Ingrese un código de participante, por ejemplo P01.")
                    null
                } else {
                    ExperimentSetupEvent.StartSession(
                        ExperimentSession(
                            participantCode = cleanCode,
                            group = _state.value.selectedGroup
                        )
                    )
                }
            }
        }
    }
}
