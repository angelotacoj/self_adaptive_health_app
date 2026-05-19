package com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord

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
    var state: WellBeingState? by mutableStateOf(null)
        private set

    fun start(record: WellBeingRecord) {
        if (state?.label != record.label) {
            state = WellBeingState(label = record.label, suggestedValue = record.value, valueText = record.value.toString())
        }
    }

    fun onAction(action: WellBeingAction): WellBeingEvent? {
        val current = state ?: return null
        return when (action) {
            WellBeingAction.StartFormClicked -> {
                state = current.copy(step = WellBeingStep.Form, errorMessage = null)
                null
            }
            is WellBeingAction.ValueChanged -> {
                state = current.copy(valueText = action.value.filter { it.isDigit() }, errorMessage = null)
                null
            }
            WellBeingAction.ValidateClicked -> {
                val value = current.valueText.toIntOrNull()
                state = if (value == null || value !in 1..10) {
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
                state = current.copy(step = WellBeingStep.Review)
                null
            }
            WellBeingAction.SaveClicked -> {
                state = current.copy(step = WellBeingStep.Success)
                null
            }
            WellBeingAction.EditClicked -> {
                state = current.copy(step = WellBeingStep.Form)
                null
            }
            WellBeingAction.BackClicked -> {
                when (current.step) {
                    WellBeingStep.Intro -> return WellBeingEvent.ExitTask
                    WellBeingStep.Form -> state = current.copy(step = WellBeingStep.Intro)
                    WellBeingStep.Validation -> state = current.copy(step = WellBeingStep.Form)
                    WellBeingStep.Review -> state = current.copy(step = WellBeingStep.Validation)
                    WellBeingStep.Success -> return WellBeingEvent.ExitTask
                }
                null
            }
            WellBeingAction.CancelClicked -> WellBeingEvent.ExitTask
            is WellBeingAction.AdaptiveStateChanged -> {
                state = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
