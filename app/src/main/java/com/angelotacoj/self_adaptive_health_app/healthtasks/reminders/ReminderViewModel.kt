package com.angelotacoj.self_adaptive_health_app.healthtasks.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate

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
    var state: ReminderState? by mutableStateOf(null)
        private set

    fun start(template: ReminderTemplate) {
        if (state?.activity != template.activity) {
            state = ReminderState(
                activity = template.activity,
                time = template.time,
                frequency = template.frequency
            )
        }
    }

    fun onAction(action: ReminderAction): ReminderEvent? {
        val current = state ?: return null
        return when (action) {
            ReminderAction.StartNewReminderClicked -> {
                state = current.copy(step = ReminderStep.SelectActivity)
                null
            }
            ReminderAction.ActivitySelected -> {
                state = current.copy(step = ReminderStep.SelectTime)
                null
            }
            ReminderAction.TimeSelected -> {
                state = current.copy(step = ReminderStep.SelectFrequency)
                null
            }
            ReminderAction.FrequencySelected -> {
                state = current.copy(step = ReminderStep.ReviewSummary)
                null
            }
            ReminderAction.SaveReminderClicked -> {
                state = current.copy(step = ReminderStep.Saved)
                null
            }
            ReminderAction.BackClicked -> {
                when (current.step) {
                    ReminderStep.Intro -> ReminderEvent.ExitTask
                    ReminderStep.SelectActivity -> state = current.copy(step = ReminderStep.Intro)
                    ReminderStep.SelectTime -> state = current.copy(step = ReminderStep.SelectActivity)
                    ReminderStep.SelectFrequency -> state = current.copy(step = ReminderStep.SelectTime)
                    ReminderStep.ReviewSummary -> state = current.copy(step = ReminderStep.SelectFrequency)
                    ReminderStep.Saved -> return ReminderEvent.ExitTask
                }
                null
            }
            ReminderAction.CancelClicked -> ReminderEvent.ExitTask
            is ReminderAction.AdaptiveStateChanged -> {
                state = current.copy(adaptiveUiState = action.adaptiveUiState)
                null
            }
        }
    }
}
