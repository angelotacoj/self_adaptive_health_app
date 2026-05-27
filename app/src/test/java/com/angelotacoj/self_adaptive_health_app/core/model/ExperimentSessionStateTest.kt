package com.angelotacoj.self_adaptive_health_app.core.model

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.data.FakeHealthDataSource
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentSessionStateTest {
    @Test
    fun groupACompletesExactlyTenTasks_staticThenSelfAdaptive() {
        val finished = completeTenTasks(ExperimentGroup.GroupA)

        assertEquals(ExperimentTasksPerCondition, finished.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(ExperimentTasksPerCondition, finished.completedTasksByCondition.getValue(ExperimentCondition.SELF_ADAPTIVE_UI).size)
        assertEquals(finished.totalRequiredTaskRuns(), finished.completedTaskCount())
        assertFalse(finished.isSessionActive)
    }

    @Test
    fun groupBCompletesExactlyTenTasks_selfAdaptiveThenStatic() {
        val finished = completeTenTasks(ExperimentGroup.GroupB)

        assertEquals(ExperimentTasksPerCondition, finished.completedTasksByCondition.getValue(ExperimentCondition.SELF_ADAPTIVE_UI).size)
        assertEquals(ExperimentTasksPerCondition, finished.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(finished.totalRequiredTaskRuns(), finished.completedTaskCount())
        assertFalse(finished.isSessionActive)
    }

    @Test
    fun completingSameTaskTwiceInSameConditionDoesNotIncreaseCount() {
        val dataSet = FakeHealthDataSource().getDataSet(ExperimentGroup.GroupA)
        var state = ExperimentSessionState(
            participantId = "UNIT_DUPLICATE",
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
            participantId = "UNIT_SECOND_CONDITION",
            group = ExperimentGroup.GroupA,
            conditionOrder = ExperimentGroup.GroupA.conditionOrder(),
            currentDataSet = dataSet
        )

        ExperimentTaskOrder.forEach { taskId ->
            state = state.startTask(taskId).finishCurrentTask()
        }
        state = state.moveToNextCondition()

        assertEquals(ExperimentTasksPerCondition, state.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(emptySet<TaskId>(), state.completedTasksByCondition[ExperimentCondition.SELF_ADAPTIVE_UI].orEmpty())
        assertEquals(ExperimentCondition.SELF_ADAPTIVE_UI, state.currentCondition)
    }

    @Test
    fun task4IsAvailableInSelfAdaptiveAfterCompletingFirstThreeSelfAdaptiveTasks() {
        val dataSet = FakeHealthDataSource().getDataSet(ExperimentGroup.GroupA)
        var state = ExperimentSessionState(
            participantId = "UNIT_T4_AVAILABLE",
            group = ExperimentGroup.GroupA,
            conditionOrder = ExperimentGroup.GroupA.conditionOrder(),
            currentDataSet = dataSet
        )

        ExperimentTaskOrder.forEach { taskId ->
            state = state.startTask(taskId).finishCurrentTask()
        }
        state = state.moveToNextCondition()
        listOf(TaskId.T1_ACCESS, TaskId.T2_APPOINTMENT, TaskId.T3_WELL_BEING).forEach { taskId ->
            state = state.startTask(taskId).finishCurrentTask()
        }

        assertTrue(state.isTaskAvailableInCurrentCondition(TaskId.T4_REMINDER))
    }

    private fun completeTenTasks(group: ExperimentGroup): ExperimentSessionState {
        val dataSet = FakeHealthDataSource().getDataSet(group)
        var state = ExperimentSessionState(
            participantId = "UNIT_${group.name}",
            group = group,
            conditionOrder = group.conditionOrder(),
            currentDataSet = dataSet
        )
        repeat(group.conditionOrder().size) { conditionIndex ->
            assertEquals(conditionIndex, state.currentConditionIndex)
            ExperimentTaskOrder.forEach { taskId ->
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
}
