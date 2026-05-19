package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession

data class ExperimentSetupState(
    val participantCode: String = "P01",
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
    var state by mutableStateOf(ExperimentSetupState())
        private set

    fun onAction(action: ExperimentSetupAction): ExperimentSetupEvent? {
        return when (action) {
            is ExperimentSetupAction.ParticipantCodeChanged -> {
                state = state.copy(participantCode = action.value.uppercase(), errorMessage = null)
                null
            }

            is ExperimentSetupAction.GroupSelected -> {
                state = state.copy(selectedGroup = action.group, errorMessage = null)
                null
            }

            ExperimentSetupAction.StartSessionClicked -> {
                val cleanCode = state.participantCode.trim()
                if (cleanCode.isBlank()) {
                    state = state.copy(errorMessage = "Ingrese un código de participante, por ejemplo P01.")
                    null
                } else {
                    ExperimentSetupEvent.StartSession(
                        ExperimentSession(
                            participantCode = cleanCode,
                            group = state.selectedGroup
                        )
                    )
                }
            }
        }
    }
}
