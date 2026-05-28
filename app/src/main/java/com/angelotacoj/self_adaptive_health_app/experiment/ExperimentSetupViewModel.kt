package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase C1.5: Group selection has been removed from the setup flow.
 *
 * All participants follow the fixed order: STATIC_UI → SELF_ADAPTIVE_UI.
 * [ExperimentGroup.GroupA] is stored as a stable legacy value for export/traceability.
 * The UI no longer shows group picker cards.
 */
data class ExperimentSetupState(
    val participantSuffix: String = "",
    val errorMessage: String? = null
) {
    /** Fixed legacy group – stored for data traceability, not used for ordering. */
    val legacyGroup: ExperimentGroup = ExperimentGroup.GroupA
}

sealed interface ExperimentSetupAction {
    data class ParticipantSuffixChanged(val value: String) : ExperimentSetupAction
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

            ExperimentSetupAction.StartSessionClicked -> {
                val code = _state.value.participantSuffix.trim()
                if (code.length != 4 || !code.all { it.isLetterOrDigit() }) {
                    _state.value = _state.value.copy(errorMessage = "Ingrese exactamente 4 caracteres alfanuméricos.")
                    null
                } else {
                    ExperimentSetupEvent.StartSession(
                        suffix = code,
                        group = _state.value.legacyGroup   // always GroupA (fixed order)
                    )
                }
            }
        }
    }
}
