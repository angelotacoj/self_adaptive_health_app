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
            sessionId = "0001_1000",
            participantId = "0001",
            group = "GroupA",
            conditionOrder = "STATIC_UI,SELF_ADAPTIVE_UI",
            startedAt = 1000L,
            endedAt = null,
            isCompleted = false
        )
        dao.insertParticipantSession(session)

        val retrieved = dao.getSessionById("0001_1000")
        assertNotNull(retrieved)
        assertEquals("0001", retrieved?.participantId)
    }

    @Test
    fun `insert and count interaction events`() = runTest {
        val event = InteractionEventEntity(
            eventId = "E001",
            sessionId = "0001_1000",
            participantId = "0001",
            condition = "STATIC_UI",
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
            sessionId = "0001_1000",
            participantId = "0001",
            condition = "STATIC_UI",
            taskId = "T1",
            dataSet = "SET_A",
            startedAt = 1000L,
            endedAt = 2000L,
            completed = true
        )
        dao.insertTaskRun(taskRun)

        val completed = dao.isTaskCompleted("0001_1000", "STATIC_UI", "T1")
        assertTrue(completed)
    }

    @Test
    fun `deleteSessionCascade removes all related data`() = runTest {
        val sessionId = "0001_1000"
        dao.insertParticipantSession(ParticipantSessionEntity(sessionId, "0001", "GroupA", "STATIC_UI,SELF_ADAPTIVE_UI", 0, null, false))
        dao.insertInteractionEvent(InteractionEventEntity("E1", sessionId, "0001", "STATIC_UI", "T1", "S1", "T", null, 0, "M", null))
        
        assertEquals(1, dao.participantSessionCount())
        assertEquals(1, dao.interactionEventCount())
        
        dao.deleteSessionCascade(sessionId)
        
        assertEquals(0, dao.participantSessionCount())
        assertEquals(0, dao.interactionEventCount())
    }
}
