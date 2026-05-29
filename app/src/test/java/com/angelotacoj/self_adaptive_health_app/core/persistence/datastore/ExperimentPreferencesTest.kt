package com.angelotacoj.self_adaptive_health_app.core.persistence.datastore

import androidx.test.core.app.ApplicationProvider
import com.angelotacoj.self_adaptive_health_app.MainDispatcherRule
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord
import com.angelotacoj.self_adaptive_health_app.core.model.conditionOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExperimentPreferencesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferences: ExperimentPreferences
    
    private val dataSet = FakeHealthDataSet(
        id = "SET_A",
        accessCredentials = AccessCredentials("U", "P"),
        appointment = Appointment("T", "D", "T", "I", "P", "S", "L", "P", "I", "A"),
        appointmentOptions = emptyList(),
        wellBeingRecord = WellBeingRecord("L", 1),
        reminder = ReminderTemplate("A", "T", "F")
    )

    @Before
    fun setup() {
        preferences = ExperimentPreferences(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `initial snapshot is empty`() = runTest {
        val snapshot = preferences.sessionSnapshot.first()
        assertFalse(snapshot.isSessionActive)
        assertEquals(null, snapshot.participantId)
    }

    @Test
    fun `saveSession updates snapshot`() = runTest {
        val state = ExperimentSessionState(
            participantId = "P001",
            group = ExperimentGroup.GroupA,
            conditionOrder = ExperimentGroup.GroupA.conditionOrder(),
            currentDataSet = dataSet,
            isSessionActive = true
        )

        preferences.saveSession(state)

        val snapshot = preferences.sessionSnapshot.first()
        assertTrue(snapshot.isSessionActive)
        assertEquals("P001", snapshot.participantId)
        assertEquals("GroupA", snapshot.group)
        assertEquals("STATIC_UI", snapshot.currentCondition)
        assertEquals("SET_A", snapshot.currentDataSet)
    }

    @Test
    fun `clearSession resets snapshot`() = runTest {
        val state = ExperimentSessionState(
            participantId = "P001",
            group = ExperimentGroup.GroupA,
            conditionOrder = ExperimentGroup.GroupA.conditionOrder(),
            currentDataSet = dataSet,
            isSessionActive = true
        )
        preferences.saveSession(state)
        assertTrue(preferences.sessionSnapshot.first().isSessionActive)

        preferences.clearSession()

        val snapshot = preferences.sessionSnapshot.first()
        assertFalse(snapshot.isSessionActive)
        assertEquals(null, snapshot.participantId)
    }
}
