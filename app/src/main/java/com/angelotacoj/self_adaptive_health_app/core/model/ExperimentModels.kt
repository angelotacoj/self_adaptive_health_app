package com.angelotacoj.self_adaptive_health_app.core.model

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId

/**
 * LEGACY METADATA ONLY (Phase C1.5+).
 *
 * ExperimentGroup is retained in the database schema and log entries
 * for traceability/export purposes, but it NO LONGER determines condition order.
 * All participants follow the fixed order defined in [FIXED_CONDITION_ORDER].
 *
 * GroupA / GroupB are kept so existing Room rows remain valid.
 * The value stored for new sessions is [ExperimentGroup.GroupA] by convention.
 * Do NOT use this enum to derive condition sequencing.
 */
enum class ExperimentGroup(
    val label: String,
) {
    GroupA(
        label = "Grupo A",
    ),
    GroupB(
        label = "Grupo B",
    )
}

data class ExperimentSession(
    val participantId: String,
    val group: ExperimentGroup
)

data class ExperimentSessionState(
    val participantId: String,
    val group: ExperimentGroup,
    val conditionOrder: List<ExperimentCondition>,
    val currentConditionIndex: Int = 0,
    val currentTaskId: TaskId? = null,
    val currentDataSet: FakeHealthDataSet,
    val completedTasksByCondition: Map<ExperimentCondition, Set<TaskId>> = emptyMap(),
    val sessionStartedAt: Long = System.currentTimeMillis(),
    val isSessionActive: Boolean = true,
    val isProfileCompleted: Boolean = false
) {
    val currentCondition: ExperimentCondition
        get() = conditionOrder[currentConditionIndex]

    val sessionId: String
        get() = "${participantId}_${sessionStartedAt}"

    fun cancelSession(): ExperimentSessionState = copy(isSessionActive = false, currentTaskId = null)

    fun startTask(taskId: TaskId): ExperimentSessionState = copy(currentTaskId = taskId)

    fun finishCurrentTask(): ExperimentSessionState {
        val task = currentTaskId ?: return this
        val completed = completedTasksByCondition[currentCondition].orEmpty() + task
        return copy(
            currentTaskId = null,
            completedTasksByCondition = completedTasksByCondition + (currentCondition to completed)
        )
    }

    fun moveToNextCondition(): ExperimentSessionState {
        val nextIndex = currentConditionIndex + 1
        return if (nextIndex in conditionOrder.indices) {
            copy(currentConditionIndex = nextIndex, currentTaskId = null)
        } else {
            finishSession()
        }
    }

    fun finishSession(): ExperimentSessionState = copy(isSessionActive = false, currentTaskId = null)
}

data class Appointment(
    val title: String,
    val date: String,
    val time: String,
    val instruction: String,
    val professionalName: String,
    val specialty: String,
    val location: String,
    val preparation: String,
    val itemsToBring: String,
    val accessibilityNote: String,
    val simulationDisclaimer: String = "Documento ficticio - Solo para fines de simulación"
)

data class AccessCredentials(
    val userCode: String,
    val simulatedPin: String
)

data class WellBeingRecord(
    val label: String,
    val value: Int
)

data class ReminderTemplate(
    val activity: String,
    val time: String,
    val frequency: String
)

data class FakeHealthDataSet(
    val id: String,
    val accessCredentials: AccessCredentials,
    val appointment: Appointment,
    val appointmentOptions: List<Appointment>,
    val wellBeingRecord: WellBeingRecord,
    val reminder: ReminderTemplate
)

val ExperimentTaskOrder: List<TaskId> = listOf(
    TaskId.T1_ACCESS,
    TaskId.T2_APPOINTMENT,
    TaskId.T3_WELL_BEING,
    TaskId.T4_REMINDER,
    TaskId.T5_SUMMARY
)

val ExperimentTasksPerCondition: Int
    get() = ExperimentTaskOrder.size

fun ExperimentSessionState.totalRequiredTaskRuns(): Int = ExperimentTaskOrder.size * conditionOrder.size

fun ExperimentSessionState.completedTaskCount(): Int = completedTasksByCondition.values.sumOf { it.size }

fun ExperimentSessionState.isCurrentConditionComplete(): Boolean {
    return completedTasksByCondition[currentCondition].orEmpty().containsAll(ExperimentTaskOrder)
}

fun ExperimentSessionState.isTaskCompletedInCurrentCondition(taskId: TaskId): Boolean {
    return taskId in completedTasksByCondition[currentCondition].orEmpty()
}

fun ExperimentSessionState.isTaskAvailableInCurrentCondition(taskId: TaskId): Boolean {
    if (isTaskCompletedInCurrentCondition(taskId)) return false
    val index = ExperimentTaskOrder.indexOf(taskId)
    if (index < 0) return false
    if (index == 0) return true
    val completed = completedTasksByCondition[currentCondition].orEmpty()
    return ExperimentTaskOrder.take(index).all { it in completed }
}

fun ExperimentSessionState.isExperimentComplete(): Boolean {
    return conditionOrder.all { condition ->
        completedTasksByCondition[condition].orEmpty().containsAll(ExperimentTaskOrder)
    }
}

/**
 * Phase C1.5 – Fixed condition order for all participants.
 *
 * Within-subjects, no counterbalancing:
 *  1. STATIC_UI   (then UEQ)
 *  2. SELF_ADAPTIVE_UI (then UEQ + short interview)
 */
val FIXED_CONDITION_ORDER: List<ExperimentCondition> =
    listOf(
        ExperimentCondition.STATIC_UI,
        ExperimentCondition.SELF_ADAPTIVE_UI
    )

/**
 * Always returns [FIXED_CONDITION_ORDER] regardless of group.
 *
 * The `group` field is retained as legacy export metadata only.
 * Do NOT add new behaviour that branches on ExperimentGroup.
 */
fun ExperimentGroup.conditionOrder(): List<ExperimentCondition> =
    FIXED_CONDITION_ORDER