package com.angelotacoj.self_adaptive_health_app.healthtasks.summary

import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryViewModelTest {

    private val dataSet = FakeHealthDataSet(
        id = "SET001",
        accessCredentials = AccessCredentials("USER123", "1234"),
        appointment = Appointment("Cita Dental", "20/05/2026", "10:00 AM", "Traer carnet"),
        appointmentOptions = listOf(
            Appointment("Cita Dental", "20/05/2026", "10:00 AM", "Traer carnet"),
            Appointment("Cita Dental", "21/05/2026", "11:00 AM", "Traer carnet")
        ),
        wellBeingRecord = WellBeingRecord("Nivel de energía", 5),
        reminder = ReminderTemplate("Caminar", "08:00 AM", "Diario")
    )
    private val viewModel = SummaryViewModel()

    @Test
    fun `initial state is null before start`() {
        assertNull(viewModel.state.value)
    }

    @Test
    fun `start initializes state with dataSet and Intro step`() {
        viewModel.start(dataSet)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals("SET001", state?.dataSet?.id)
        assertEquals(SummaryStep.Intro, state?.step)
    }

    @Test
    fun `StartReviewClicked moves to Details step`() {
        viewModel.start(dataSet)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        assertEquals(SummaryStep.Details, viewModel.state.value?.step)
    }

    @Test
    fun `SaveInformationClicked moves to ReinforcedConfirmation step`() {
        viewModel.start(dataSet)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        assertEquals(SummaryStep.ReinforcedConfirmation, viewModel.state.value?.step)
    }

    @Test
    fun `ConfirmClicked moves to Final step with Confirmed result`() {
        viewModel.start(dataSet)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        viewModel.onAction(SummaryAction.ConfirmClicked)
        
        val state = viewModel.state.value
        assertEquals(SummaryStep.Final, state?.step)
        assertEquals(SummaryResult.Confirmed, state?.result)
    }

    @Test
    fun `EditClicked moves back to Details step and clears result`() {
        viewModel.start(dataSet)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        viewModel.onAction(SummaryAction.EditClicked)
        
        val state = viewModel.state.value
        assertEquals(SummaryStep.Details, state?.step)
        assertNull(state?.result)
    }

    @Test
    fun `CancelClicked moves to Final step with Cancelled result`() {
        viewModel.start(dataSet)
        viewModel.onAction(SummaryAction.CancelClicked)
        
        val state = viewModel.state.value
        assertEquals(SummaryStep.Final, state?.step)
        assertEquals(SummaryResult.Cancelled, state?.result)
    }

    @Test
    fun `BackClicked moves back through steps`() {
        viewModel.start(dataSet)
        
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        assertEquals(SummaryStep.ReinforcedConfirmation, viewModel.state.value?.step)
        
        viewModel.onAction(SummaryAction.BackClicked)
        assertEquals(SummaryStep.Details, viewModel.state.value?.step)
        
        viewModel.onAction(SummaryAction.BackClicked)
        assertEquals(SummaryStep.Intro, viewModel.state.value?.step)
        
        val event = viewModel.onAction(SummaryAction.BackClicked)
        assertEquals(SummaryEvent.ExitTask, event)
    }
}
