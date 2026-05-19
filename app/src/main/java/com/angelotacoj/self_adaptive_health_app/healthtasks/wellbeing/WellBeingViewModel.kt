package com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WellBeingStep {
    Intro,
    Form,
    Validation,
    Review,
    Success
}

data class WellBeingState(
    val step: WellBeingStep = WellBeingStep.Intro,
    val label: String,
    val suggestedValue: Int,
    val valueText: String = "",
    val errorMessage: String? = null,
    val fieldErrorCount: Int = 0,
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface WellBeingAction {
    data object StartFormClicked : WellBeingAction
    data class ValueChanged(val value: String) : WellBeingAction
    data object ValidateClicked : WellBeingAction
    data object ContinueToReviewClicked : WellBeingAction
    data object SaveClicked : WellBeingAction
    data object EditClicked : WellBeingAction
    data object BackClicked : WellBeingAction
    data object CancelClicked : WellBeingAction
    data class AdaptiveStateChanged(val adaptiveUiState: AdaptiveUiState) : WellBeingAction
}

sealed interface WellBeingEvent {
    data object ExitTask : WellBeingEvent
}

class WellBeingViewModel : ViewModel() {
    private val _state = MutableStateFlow<WellBeingState?>(null)
    val state: StateFlow<WellBeingState?> = _state.asStateFlow()

    fun start(record: WellBeingRecord) {
        if (_state.value?.label != record.label) {
            _state.value = WellBeingState(label = record.label, suggestedValue = record.value, valueText = record.value.toString())
        }
    }

    fun onAction(action: WellBeingAction): WellBeingEvent? {
        val current = _state.value ?: return null
        return when (action) {
            WellBeingAction.StartFormClicked -> {
                _state.value = current.copy(step = WellBeingStep.Form, errorMessage = null)
                null
            }
            is WellBeingAction.ValueChanged -> {
                _state.value = current.copy(valueText = action.value.filter { it.isDigit() }, errorMessage = null)
                null
            }
            WellBeingAction.ValidateClicked -> {
                val value = current.valueText.toIntOrNull()
                _state.value = if (value == null || value !in 1..10) {
                    current.copy(
                        step = WellBeingStep.Form,
                        errorMessage = "Ingrese un valor ficticio del 1 al 10.",
                        fieldErrorCount = current.fieldErrorCount + 1
                    )
                } else {
                    current.copy(step = WellBeingStep.Validation, errorMessage = null)
                }
                null
            }
            WellBeingAction.ContinueToReviewClicked -> {
                _state.value = current.copy(step = WellBeingStep.Review)
                null
            }
            WellBeingAction.SaveClicked -> {
                _state.value = current.copy(step = WellBeingStep.Success)
                null
            }
            WellBeingAction.EditClicked -> {
                _state.value = current.copy(step = WellBeingStep.Form)
                null
            }
            WellBeingAction.BackClicked -> {
                when (current.step) {
                    WellBeingStep.Intro -> return WellBeingEvent.ExitTask
                    WellBeingStep.Form -> _state.value = current.copy(step = WellBeingStep.Intro)
                    WellBeingStep.Validation -> _state.value = current.copy(step = WellBeingStep.Form)
                    WellBeingStep.Review -> _state.value = current.copy(step = WellBeingStep.Form)
                    WellBeingStep.Success -> return WellBeingEvent.ExitTask
                }
                null
            }
            WellBeingAction.CancelClicked -> WellBeingEvent.ExitTask
            is WellBeingAction.AdaptiveStateChanged -> {
                _state.value = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
