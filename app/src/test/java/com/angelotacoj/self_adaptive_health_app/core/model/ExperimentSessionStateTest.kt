package com.angelotacoj.self_adaptive_health_app.core.model

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.data.FakeHealthDataSource
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExperimentSessionStateTest {
    @Test
    fun groupACompletesExactlyEightTasks_staticThenSelfAdaptive() {
        val finished = completeEightTasks(ExperimentGroup.GroupA)

        assertEquals(4, finished.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(4, finished.completedTasksByCondition.getValue(ExperimentCondition.SELF_ADAPTIVE_UI).size)
        assertEquals(8, finished.completedTasksByCondition.values.sumOf { it.size })
        assertFalse(finished.isSessionActive)
    }

    @Test
    fun groupBCompletesExactlyEightTasks_selfAdaptiveThenStatic() {
        val finished = completeEightTasks(ExperimentGroup.GroupB)

        assertEquals(4, finished.completedTasksByCondition.getValue(ExperimentCondition.SELF_ADAPTIVE_UI).size)
        assertEquals(4, finished.completedTasksByCondition.getValue(ExperimentCondition.STATIC_UI).size)
        assertEquals(8, finished.completedTasksByCondition.values.sumOf { it.size })
        assertFalse(finished.isSessionActive)
    }

    private fun completeEightTasks(group: ExperimentGroup): ExperimentSessionState {
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
        TaskId.T2_WELL_BEING,
        TaskId.T3_REMINDER,
        TaskId.T4_SUMMARY
    )
}
