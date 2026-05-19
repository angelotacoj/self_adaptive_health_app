package com.angelotacoj.self_adaptive_health_app.core.logging

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import java.util.UUID

enum class InteractionEventType {
    SESSION_STARTED,
    SESSION_CANCELLED,
    CONDITION_STARTED,
    CONDITION_COMPLETED,
    TASK_STARTED,
    TASK_COMPLETED,
    SCREEN_ENTERED,
    SCREEN_EXITED,
    BUTTON_CLICKED,
    HELP_REQUESTED,
    BACK_PRESSED,
    FIELD_ERROR,
    TOUCH_ERROR,
    LONG_TIME_TRIGGERED,
    CONFIRMATION_PAUSE,
    SENSITIVE_ACTION,
    ADAPTATION_SUGGESTED,
    ADAPTATION_APPLIED,
    ADAPTATION_REJECTED,
    ADAPTATION_UNDONE
}

enum class TaskId(val label: String) {
    T1_APPOINTMENT("T1 Medical appointment"),
    T2_WELL_BEING("T2 Well-being record"),
    T3_REMINDER("T3 Reminder"),
    T4_SUMMARY("T4 Review and confirm")
}

enum class ScreenId {
    EXPERIMENT_SETUP,
    HOME,
    APPOINTMENT_OVERVIEW,
    APPOINTMENT_LIST,
    APPOINTMENT_DETAIL,
    APPOINTMENT_CONFIRMATION,
    APPOINTMENT_COMPLETED,
    WELL_BEING_INTRO,
    WELL_BEING_FORM,
    WELL_BEING_VALIDATION,
    WELL_BEING_REVIEW,
    WELL_BEING_SUCCESS,
    REMINDER_INTRO,
    REMINDER_NEW,
    REMINDER_ACTIVITY,
    REMINDER_TIME,
    REMINDER_FREQUENCY,
    REMINDER_REVIEW,
    REMINDER_SAVED,
    SUMMARY_INTRO,
    SUMMARY_REVIEW,
    SUMMARY_CONFIRMATION,
    SUMMARY_FINAL,
    DEBUG_LOGS
}

data class DebugLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val participantCode: String,
    val group: ExperimentGroup,
    val condition: ExperimentCondition = ExperimentCondition.STATIC_UI,
    val taskId: TaskId? = null,
    val screenId: ScreenId? = null,
    val eventType: InteractionEventType,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)

interface ExperimentLogger {
    fun log(entry: DebugLogEntry)
    fun getLogs(): List<DebugLogEntry>
    fun clear()
}

class InMemoryExperimentLogger : ExperimentLogger {
    private val entries = mutableListOf<DebugLogEntry>()

    override fun log(entry: DebugLogEntry) {
        entries += entry
    }

    override fun getLogs(): List<DebugLogEntry> = entries.toList()

    override fun clear() {
        entries.clear()
    }
}
