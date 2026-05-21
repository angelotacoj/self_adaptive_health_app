package com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository

import com.angelotacoj.self_adaptive_health_app.MainDispatcherRule
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.InteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.persistence.datastore.ExperimentPreferences
import com.angelotacoj.self_adaptive_health_app.core.persistence.datastore.SessionPreferenceSnapshot
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDao
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PersistentKnowledgeRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = mockk<ExperimentPreferences>()
    private val dao = mockk<ExperimentDao>(relaxed = true)
    private lateinit var repository: PersistentKnowledgeRepository

    @Before
    fun setup() {
        coEvery { preferences.sessionSnapshot } returns flowOf(
            SessionPreferenceSnapshot(
                participantCode = "P001",
                currentDataSet = "SET_A",
                currentCondition = "SELF_ADAPTIVE_UI",
                isSessionActive = true
            )
        )
        repository = PersistentKnowledgeRepository(preferences, dao)
    }

    @Test
    fun `getCurrentSession returns formatted string from preferences`() = runTest {
        val session = repository.getCurrentSession()
        assertEquals("P001_SET_A", session)
    }

    @Test
    fun `getCurrentCondition returns value from preferences`() = runTest {
        val condition = repository.getCurrentCondition()
        assertEquals("SELF_ADAPTIVE_UI", condition)
    }

    @Test
    fun `saveInteractionEvent calls dao with mapped entity`() = runTest {
        val entry = DebugLogEntry(
            id = "EVT001",
            participantCode = "P001",
            group = ExperimentGroup.GroupA,
            condition = ExperimentCondition.SELF_ADAPTIVE_UI,
            taskId = TaskId.T1_ACCESS,
            screenId = ScreenId.ACCESS_CODE,
            eventType = InteractionEventType.BUTTON_CLICKED,
            message = "Test click",
            timestamp = 123456789L
        )

        repository.saveInteractionEvent(entry, oisCode = "OIS123")

        coVerify {
            dao.insertInteractionEvent(match {
                it.eventId == "EVT001" &&
                it.sessionId == "P001_SET_A" &&
                it.participantCode == "P001" &&
                it.condition == "SELF_ADAPTIVE_UI" &&
                it.taskId == "T1_ACCESS" &&
                it.screenId == "ACCESS_CODE" &&
                it.eventType == "BUTTON_CLICKED" &&
                it.oisCode == "OIS123" &&
                it.message == "Test click"
            })
        }
    }

    @Test
    fun `saveAdaptationEvent calls dao`() = runTest {
        val entity = AdaptationEventEntity(
            adaptationEventId = "AD001",
            sessionId = "S001",
            participantCode = "P001",
            condition = "SELF_ADAPTIVE_UI",
            taskId = "T1",
            screenId = "S1",
            ruleId = "AR01",
            inferredDifficulty = "D1",
            uiModifications = "M1",
            validationType = "V1",
            systemDecision = "SD1",
            userDecision = "UD1",
            applied = true,
            undone = false,
            timestamp = 123L
        )

        repository.saveAdaptationEvent(entity)

        coVerify { dao.insertAdaptationEvent(entity) }
    }

    @Test
    fun `saveUserDecision calls dao`() = runTest {
        val entity = UserDecisionEventEntity(
            decisionId = "DEC001",
            adaptationEventId = "AD001",
            sessionId = "S001",
            participantCode = "P001",
            taskId = "T1",
            screenId = "S1",
            decision = "CONFIRM",
            timestamp = 123L
        )

        repository.saveUserDecision(entity)

        coVerify { dao.insertUserDecisionEvent(entity) }
    }

    @Test
    fun `clearCurrentSession clears preferences and in-memory memory`() = runTest {
        coEvery { preferences.clearSession() } returns Unit
        
        // Populate in-memory memory
        repository.rememberRejected(TaskId.T1_ACCESS, AdaptationRuleId.AR02)
        assertTrue(repository.wasRejectedInCurrentTask(TaskId.T1_ACCESS, AdaptationRuleId.AR02))
        
        repository.clearCurrentSession()
        
        coVerify { preferences.clearSession() }
        val snapshot = repository.snapshot(TaskId.T1_ACCESS, ScreenId.ACCESS_CODE)
        assertTrue(snapshot.rejectedRulesForTask.isEmpty())
    }
}
