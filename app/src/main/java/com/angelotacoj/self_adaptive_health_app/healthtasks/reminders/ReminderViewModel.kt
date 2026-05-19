package com.angelotacoj.self_adaptive_health_app.healthtasks.reminders

import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReminderStep {
    Intro,
    SelectActivity,
    SelectTime,
    SelectFrequency,
    ReviewSummary,
    Saved
}

data class ReminderState(
    val step: ReminderStep = ReminderStep.Intro,
    val activity: String,
    val time: String,
    val frequency: String,
    val adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
)

sealed interface ReminderAction {
    data object StartNewReminderClicked : ReminderAction
    data object ActivitySelected : ReminderAction
    data object TimeSelected : ReminderAction
    data object FrequencySelected : ReminderAction
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
                _state.value = current.copy(step = ReminderStep.SelectActivity)
                null
            }
            ReminderAction.ActivitySelected -> {
                _state.value = current.copy(step = ReminderStep.SelectTime)
                null
            }
            ReminderAction.TimeSelected -> {
                _state.value = current.copy(step = ReminderStep.SelectFrequency)
                null
            }
            ReminderAction.FrequencySelected -> {
                _state.value = current.copy(step = ReminderStep.ReviewSummary)
                null
            }
            ReminderAction.SaveReminderClicked -> {
                _state.value = current.copy(step = ReminderStep.Saved)
                null
            }
            ReminderAction.BackClicked -> {
                when (current.step) {
                    ReminderStep.Intro -> ReminderEvent.ExitTask
                    ReminderStep.SelectActivity -> _state.value = current.copy(step = ReminderStep.Intro)
                    ReminderStep.SelectTime -> _state.value = current.copy(step = ReminderStep.SelectActivity)
                    ReminderStep.SelectFrequency -> _state.value = current.copy(step = ReminderStep.SelectTime)
                    ReminderStep.ReviewSummary -> _state.value = current.copy(step = ReminderStep.SelectFrequency)
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
