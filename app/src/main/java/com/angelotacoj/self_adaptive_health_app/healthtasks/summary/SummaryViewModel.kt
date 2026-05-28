package com.angelotacoj.self_adaptive_health_app.healthtasks.summary

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SummaryStep {
    Intro,
    Details,
    ReinforcedConfirmation,
    Final
}

enum class SummaryResult {
    Confirmed,
    Edited,
    Cancelled
}

data class SummaryState(
    val taskOutputs: Map<String, String>,
    val step: SummaryStep = SummaryStep.Intro,
    val result: SummaryResult? = null,
    val editNote: String = "",
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface SummaryAction {
    data object StartReviewClicked : SummaryAction
    data object SaveInformationClicked : SummaryAction
    data object ConfirmClicked : SummaryAction
    data object EditClicked : SummaryAction
    data object CancelClicked : SummaryAction
    data object BackClicked : SummaryAction
    data class AdaptiveStateChanged(val adaptiveUiState: AdaptiveUiState) : SummaryAction
}

sealed interface SummaryEvent {
    data object ExitTask : SummaryEvent
}

class SummaryViewModel : ViewModel() {
    private val _state = MutableStateFlow<SummaryState?>(null)
    val state: StateFlow<SummaryState?> = _state.asStateFlow()

    fun start(taskOutputs: Map<String, String>) {
        if (_state.value?.taskOutputs != taskOutputs) {
            _state.value = SummaryState(taskOutputs = taskOutputs)
        }
    }

    fun onAction(action: SummaryAction): SummaryEvent? {
        val current = _state.value ?: return null
        return when (action) {
            SummaryAction.StartReviewClicked -> {
                _state.value = current.copy(step = SummaryStep.Details)
                null
            }
            SummaryAction.SaveInformationClicked -> {
                _state.value = current.copy(step = SummaryStep.ReinforcedConfirmation)
                null
            }
            SummaryAction.ConfirmClicked -> {
                _state.value = current.copy(step = SummaryStep.Final, result = SummaryResult.Confirmed)
                null
            }
            SummaryAction.EditClicked -> {
                _state.value = current.copy(step = SummaryStep.Details, result = null, editNote = "El participante seleccionó Editar. No se modificaron datos reales.")
                null
            }
            SummaryAction.CancelClicked -> {
                _state.value = current.copy(step = SummaryStep.Final, result = SummaryResult.Cancelled)
                null
            }
            SummaryAction.BackClicked -> {
                when (current.step) {
                    SummaryStep.Intro -> return SummaryEvent.ExitTask
                    SummaryStep.Details -> _state.value = current.copy(step = SummaryStep.Intro)
                    SummaryStep.ReinforcedConfirmation -> _state.value = current.copy(step = SummaryStep.Details)
                    SummaryStep.Final -> return SummaryEvent.ExitTask
                }
                null
            }
            is SummaryAction.AdaptiveStateChanged -> {
                _state.value = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
