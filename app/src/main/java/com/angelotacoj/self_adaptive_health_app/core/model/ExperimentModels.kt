package com.angelotacoj.self_adaptive_health_app.core.model

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId

enum class ExperimentGroup(
    val label: String,
    val orderDescription: String,
    val dataSetId: String
) {
    GroupA(
        label = "Grupo A",
        orderDescription = "Interfaz estática -> Interfaz autoadaptativa",
        dataSetId = "Conjunto A"
    ),
    GroupB(
        label = "Grupo B",
        orderDescription = "Interfaz autoadaptativa -> Interfaz estática",
        dataSetId = "Conjunto B"
    )
}

data class ExperimentSession(
    val participantCode: String,
    val group: ExperimentGroup
)

data class ExperimentSessionState(
    val participantCode: String,
    val group: ExperimentGroup,
    val conditionOrder: List<ExperimentCondition>,
    val currentConditionIndex: Int = 0,
    val currentTaskId: TaskId? = null,
    val currentDataSet: FakeHealthDataSet,
    val completedTasksByCondition: Map<ExperimentCondition, Set<TaskId>> = emptyMap(),
    val sessionStartedAt: Long = System.currentTimeMillis(),
    val isSessionActive: Boolean = true
) {
    val currentCondition: ExperimentCondition
        get() = conditionOrder[currentConditionIndex]

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

    fun moveToNextTask(taskId: TaskId): ExperimentSessionState = copy(currentTaskId = taskId)

    fun finishCondition(): ExperimentSessionState = copy(currentTaskId = null)

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
    val instruction: String
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
    val appointment: Appointment,
    val appointmentOptions: List<Appointment>,
    val wellBeingRecord: WellBeingRecord,
    val reminder: ReminderTemplate
)

fun ExperimentGroup.conditionOrder(): List<ExperimentCondition> {
    return when (this) {
        ExperimentGroup.GroupA -> listOf(ExperimentCondition.STATIC_UI, ExperimentCondition.SELF_ADAPTIVE_UI)
        ExperimentGroup.GroupB -> listOf(ExperimentCondition.SELF_ADAPTIVE_UI, ExperimentCondition.STATIC_UI)
    }
}
