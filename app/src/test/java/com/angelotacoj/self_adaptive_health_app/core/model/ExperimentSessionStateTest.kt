package com.angelotacoj.self_adaptive_health_app.core.model

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.data.FakeHealthDataSource
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExperimentSessionStateTest {
    @Test
    fun groupACompletesExactlyTenTasks_staticThenSelfAdaptive() {
        val finished = completeTenTasks(ExperimentGroup.GroupA)

        assertEquals(5, finished.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(5, finished.completedTasksByCondition.getValue(ExperimentCondition.SELF_ADAPTIVE_UI).size)
        assertEquals(10, finished.completedTasksByCondition.values.sumOf { it.size })
        assertFalse(finished.isSessionActive)
    }

    @Test
    fun groupBCompletesExactlyTenTasks_selfAdaptiveThenStatic() {
        val finished = completeTenTasks(ExperimentGroup.GroupB)

        assertEquals(5, finished.completedTasksByCondition.getValue(ExperimentCondition.SELF_ADAPTIVE_UI).size)
        assertEquals(5, finished.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(10, finished.completedTasksByCondition.values.sumOf { it.size })
        assertFalse(finished.isSessionActive)
    }

    @Test
    fun completingSameTaskTwiceInSameConditionDoesNotIncreaseCount() {
        val dataSet = FakeHealthDataSource().getDataSet(ExperimentGroup.GroupA)
        var state = ExperimentSessionState(
            participantCode = "UNIT_DUPLICATE",
            group = ExperimentGroup.GroupA,
            conditionOrder = ExperimentGroup.GroupA.conditionOrder(),
            currentDataSet = dataSet
        )

        state = state.startTask(TaskId.T1_ACCESS).finishCurrentTask()
        state = state.startTask(TaskId.T1_ACCESS).finishCurrentTask()

        assertEquals(1, state.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
    }

    @Test
    fun movingToSecondConditionKeepsNewConditionTasksPending() {
        val dataSet = FakeHealthDataSource().getDataSet(ExperimentGroup.GroupA)
        var state = ExperimentSessionState(
            participantCode = "UNIT_SECOND_CONDITION",
            group = ExperimentGroup.GroupA,
            conditionOrder = ExperimentGroup.GroupA.conditionOrder(),
            currentDataSet = dataSet
        )

        taskSet.forEach { taskId ->
            state = state.startTask(taskId).finishCurrentTask()
        }
        state = state.moveToNextCondition()

        assertEquals(5, state.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(emptySet<TaskId>(), state.completedTasksByCondition[ExperimentCondition.SELF_ADAPTIVE_UI].orEmpty())
        assertEquals(ExperimentCondition.SELF_ADAPTIVE_UI, state.currentCondition)
    }

    private fun completeTenTasks(group: ExperimentGroup): ExperimentSessionState {
        val dataSet = FakeHealthDataSource().getDataSet(group)
        var state = ExperimentSessionState(
            participantCode = "UNIT_${group.name}",
            group = group,
            conditionOrder = group.conditionOrder(),
            currentDataSet = dataSet
        )
        repeat(group.conditionOrder().size) { conditionIndex ->
            assertEquals(conditionIndex, state.currentConditionIndex)
            taskSet.forEach { taskId ->
                state = state.startTask(taskId).finishCurrentTask()
            }
            state = if (conditionIndex == group.conditionOrder().lastIndex) {
                state.finishSession()
            } else {
                state.moveToNextCondition()
            }
        }
        return state
    }

    private val taskSet = listOf(
        TaskId.T1_ACCESS,
        TaskId.T2_APPOINTMENT,
        TaskId.T3_WELL_BEING,
        TaskId.T4_REMINDER,
        TaskId.T5_SUMMARY
    )
}
