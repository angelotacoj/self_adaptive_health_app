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
    Review,
    Success
}

data class WellBeingState(
    val step: WellBeingStep = WellBeingStep.Intro,
    val energyLevel: String = "",
    val mood: String = "",
    val note: String = "",
    val errorMessage: String? = null,
    val fieldErrorCount: Int = 0,
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface WellBeingAction {
    data object StartFormClicked : WellBeingAction
    data class EnergyLevelChanged(val value: String) : WellBeingAction
    data class MoodSelected(val mood: String) : WellBeingAction
    data class NoteChanged(val note: String) : WellBeingAction
    data object ValidateAndReviewClicked : WellBeingAction
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
        if (_state.value == null) {
            _state.value = WellBeingState()
        }
    }

    fun onAction(action: WellBeingAction): WellBeingEvent? {
        val current = _state.value ?: return null
        return when (action) {
            WellBeingAction.StartFormClicked -> {
                _state.value = current.copy(step = WellBeingStep.Form, errorMessage = null)
                null
            }
            is WellBeingAction.EnergyLevelChanged -> {
                _state.value = current.copy(energyLevel = action.value.filter { it.isDigit() }, errorMessage = null)
                null
            }
            is WellBeingAction.MoodSelected -> {
                _state.value = current.copy(mood = action.mood, errorMessage = null)
                null
            }
            is WellBeingAction.NoteChanged -> {
                _state.value = current.copy(note = action.note)
                null
            }
            WellBeingAction.ValidateAndReviewClicked -> {
                val energy = current.energyLevel.toIntOrNull()
                if (energy == null || energy !in 1..10) {
                    _state.value = current.copy(
                        errorMessage = "Ingrese un número entre 1 y 10 para el nivel de energía simulado.",
                        fieldErrorCount = current.fieldErrorCount + 1
                    )
                } else if (current.mood.isBlank()) {
                    _state.value = current.copy(
                        errorMessage = "Por favor seleccione un estado de ánimo simulado.",
                        fieldErrorCount = current.fieldErrorCount + 1
                    )
                } else {
                    _state.value = current.copy(step = WellBeingStep.Review, errorMessage = null)
                }
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
