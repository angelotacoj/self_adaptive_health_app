package com.angelotacoj.self_adaptive_health_app.core.persistence.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExperimentDaoTest {

    private lateinit var db: ExperimentDatabase
    private lateinit var dao: ExperimentDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExperimentDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.experimentDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `insert and get session`() = runTest {
        val session = ParticipantSessionEntity(
            sessionId = "S001",
            participantId = "P001",
            group = "GroupA",
            conditionOrder = "STATIC,ADAPTIVE",
            startedAt = 1000L,
            endedAt = null,
            isCompleted = false
        )
        dao.insertParticipantSession(session)

        val retrieved = dao.getSessionById("S001")
        assertNotNull(retrieved)
        assertEquals("P001", retrieved?.participantId)
    }

    @Test
    fun `insert and count interaction events`() = runTest {
        val event = InteractionEventEntity(
            eventId = "E001",
            sessionId = "S001",
            participantId = "P001",
            condition = "STATIC",
            taskId = "T1",
            screenId = "S1",
            eventType = "CLICK",
            oisCode = null,
            timestamp = 2000L,
            message = "Test message",
            metadataJson = null
        )
        dao.insertInteractionEvent(event)

        val count = dao.interactionEventCount()
        assertEquals(1, count)
        
        val recent = dao.recentInteractionEvents(1)
        assertEquals(1, recent.size)
        assertEquals("E001", recent[0].eventId)
    }

    @Test
    fun `isTaskCompleted returns correct value`() = runTest {
        val taskRun = TaskRunEntity(
            taskRunId = "TR001",
            sessionId = "S001",
            participantId = "P001",
            condition = "STATIC",
            taskId = "T1",
            dataSet = "SET_A",
            startedAt = 1000L,
            endedAt = 2000L,
            completed = true
        )
        dao.insertTaskRun(taskRun)

        val completed = dao.isTaskCompleted("S001", "STATIC", "T1")
        assertTrue(completed)
    }

    @Test
    fun `deleteSessionCascade removes all related data`() = runTest {
        val sessionId = "S001"
        dao.insertParticipantSession(ParticipantSessionEntity(sessionId, "P1", "G1", "C1", 0, null, false))
        dao.insertInteractionEvent(InteractionEventEntity("E1", sessionId, "P1", "C1", "T1", "S1", "T", null, 0, "M", null))
        
        assertEquals(1, dao.participantSessionCount())
        assertEquals(1, dao.interactionEventCount())
        
        dao.deleteSessionCascade(sessionId)
        
        assertEquals(0, dao.participantSessionCount())
        assertEquals(0, dao.interactionEventCount())
    }
}
