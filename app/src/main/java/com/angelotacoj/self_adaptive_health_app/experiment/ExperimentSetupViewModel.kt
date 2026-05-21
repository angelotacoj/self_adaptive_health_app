package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExperimentSetupState(
    val participantSuffix: String = "",
    val selectedGroup: ExperimentGroup = ExperimentGroup.GroupA,
    val errorMessage: String? = null
) {
    val selectedOrder: String = selectedGroup.orderDescription
}

sealed interface ExperimentSetupAction {
    data class ParticipantSuffixChanged(val value: String) : ExperimentSetupAction
    data class GroupSelected(val group: ExperimentGroup) : ExperimentSetupAction
    data object StartSessionClicked : ExperimentSetupAction
}

sealed interface ExperimentSetupEvent {
    data class StartSession(val suffix: String, val group: ExperimentGroup) : ExperimentSetupEvent
}

class ExperimentSetupViewModel : ViewModel() {
    private val _state = MutableStateFlow(ExperimentSetupState())
    val state: StateFlow<ExperimentSetupState> = _state.asStateFlow()

    fun onAction(action: ExperimentSetupAction): ExperimentSetupEvent? {
        return when (action) {
            is ExperimentSetupAction.ParticipantSuffixChanged -> {
                val suffix = action.value
                    .uppercase()
                    .filter { it.isLetterOrDigit() }
                    .take(4)
                _state.value = _state.value.copy(participantSuffix = suffix, errorMessage = null)
                null
            }

            is ExperimentSetupAction.GroupSelected -> {
                _state.value = _state.value.copy(selectedGroup = action.group, errorMessage = null)
                null
            }

            ExperimentSetupAction.StartSessionClicked -> {
                val suffix = _state.value.participantSuffix.trim()
                if (suffix.length != 4 || !suffix.all { it.isLetterOrDigit() }) {
                    _state.value = _state.value.copy(errorMessage = "Ingrese exactamente 4 caracteres alfanuméricos.")
                    null
                } else {
                    ExperimentSetupEvent.StartSession(
                        suffix = suffix,
                        group = _state.value.selectedGroup
                    )
                }
            }
        }
    }
}
