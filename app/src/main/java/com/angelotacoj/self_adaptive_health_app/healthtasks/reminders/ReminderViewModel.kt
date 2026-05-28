package com.angelotacoj.self_adaptive_health_app.healthtasks.reminders

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReminderStep {
    Intro,
    SelectType,
    SelectSchedule,
    SelectDetails,
    ReviewSummary,
    Saved
}

data class ReminderState(
    val step: ReminderStep = ReminderStep.Intro,
    val activity: String, // from template (what they need to configure)
    val time: String, // from template
    val frequency: String, // from template
    val selectedType: String = "",
    val selectedDate: String = "",
    val selectedTime: String = "",
    val selectedFrequency: String = "",
    val optionalLocation: String = "",
    val optionalNote: String = "",
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface ReminderAction {
    data object StartNewReminderClicked : ReminderAction
    
    // Updates
    data class TypeUpdated(val value: String) : ReminderAction
    data class DateUpdated(val value: String) : ReminderAction
    data class TimeUpdated(val value: String) : ReminderAction
    data class FrequencyUpdated(val value: String) : ReminderAction
    data class LocationUpdated(val value: String) : ReminderAction
    data class NoteUpdated(val value: String) : ReminderAction

    // Navigation
    data object TypeNextClicked : ReminderAction
    data object ScheduleNextClicked : ReminderAction
    data object DetailsNextClicked : ReminderAction
    data object SaveReminderClicked : ReminderAction
    data object BackClicked : ReminderAction
    data object CancelClicked : ReminderAction
    data class AdaptiveStateChanged(val adaptiveUiState: AdaptiveUiState) : ReminderAction
}

sealed interface ReminderEvent {
    data object ExitTask : ReminderEvent
}

class ReminderViewModel : ViewModel() {
    private val _state = MutableStateFlow<ReminderState?>(null)
    val state: StateFlow<ReminderState?> = _state.asStateFlow()

    fun start(template: ReminderTemplate) {
        if (_state.value?.activity != template.activity) {
            _state.value = ReminderState(
                activity = template.activity,
                time = template.time,
                frequency = template.frequency
            )
        }
    }

    fun onAction(action: ReminderAction): ReminderEvent? {
        val current = _state.value ?: return null
        return when (action) {
            ReminderAction.StartNewReminderClicked -> {
                _state.value = current.copy(step = ReminderStep.SelectType)
                null
            }
            is ReminderAction.TypeUpdated -> {
                _state.value = current.copy(selectedType = action.value)
                null
            }
            is ReminderAction.DateUpdated -> {
                _state.value = current.copy(selectedDate = action.value)
                null
            }
            is ReminderAction.TimeUpdated -> {
                _state.value = current.copy(selectedTime = action.value)
                null
            }
            is ReminderAction.FrequencyUpdated -> {
                _state.value = current.copy(selectedFrequency = action.value)
                null
            }
            is ReminderAction.LocationUpdated -> {
                _state.value = current.copy(optionalLocation = action.value)
                null
            }
            is ReminderAction.NoteUpdated -> {
                _state.value = current.copy(optionalNote = action.value)
                null
            }
            ReminderAction.TypeNextClicked -> {
                _state.value = current.copy(step = ReminderStep.SelectSchedule)
                null
            }
            ReminderAction.ScheduleNextClicked -> {
                _state.value = current.copy(step = ReminderStep.SelectDetails)
                null
            }
            ReminderAction.DetailsNextClicked -> {
                _state.value = current.copy(step = ReminderStep.ReviewSummary)
                null
            }
            ReminderAction.SaveReminderClicked -> {
                _state.value = current.copy(step = ReminderStep.Saved)
                null
            }
            ReminderAction.BackClicked -> {
                when (current.step) {
                    ReminderStep.Intro -> return ReminderEvent.ExitTask
                    ReminderStep.SelectType -> _state.value = current.copy(step = ReminderStep.Intro)
                    ReminderStep.SelectSchedule -> _state.value = current.copy(step = ReminderStep.SelectType)
                    ReminderStep.SelectDetails -> _state.value = current.copy(step = ReminderStep.SelectSchedule)
                    ReminderStep.ReviewSummary -> _state.value = current.copy(step = ReminderStep.SelectDetails)
                    ReminderStep.Saved -> return ReminderEvent.ExitTask
                }
                null
            }
            ReminderAction.CancelClicked -> ReminderEvent.ExitTask
            is ReminderAction.AdaptiveStateChanged -> {
                _state.value = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
