package com.angelotacoj.self_adaptive_health_app.healthtasks.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet

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
    val dataSet: FakeHealthDataSet,
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
    var state: SummaryState? by mutableStateOf(null)
        private set

    fun start(dataSet: FakeHealthDataSet) {
        if (state?.dataSet?.id != dataSet.id) {
            state = SummaryState(dataSet = dataSet)
        }
    }

    fun onAction(action: SummaryAction): SummaryEvent? {
        val current = state ?: return null
        return when (action) {
            SummaryAction.StartReviewClicked -> {
                state = current.copy(step = SummaryStep.Details)
                null
            }
            SummaryAction.SaveInformationClicked -> {
                state = current.copy(step = SummaryStep.ReinforcedConfirmation)
                null
            }
            SummaryAction.ConfirmClicked -> {
                state = current.copy(step = SummaryStep.Final, result = SummaryResult.Confirmed)
                null
            }
            SummaryAction.EditClicked -> {
                state = current.copy(step = SummaryStep.Details, result = null, editNote = "El participante seleccionó Editar. No se modificaron datos reales.")
                null
            }
            SummaryAction.CancelClicked -> {
                state = current.copy(step = SummaryStep.Final, result = SummaryResult.Cancelled)
                null
            }
            SummaryAction.BackClicked -> {
                when (current.step) {
                    SummaryStep.Intro -> return SummaryEvent.ExitTask
                    SummaryStep.Details -> state = current.copy(step = SummaryStep.Intro)
                    SummaryStep.ReinforcedConfirmation -> state = current.copy(step = SummaryStep.Details)
                    SummaryStep.Final -> return SummaryEvent.ExitTask
                }
                null
            }
            is SummaryAction.AdaptiveStateChanged -> {
                state = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
