package com.angelotacoj.self_adaptive_health_app.healthtasks.reminders

import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderViewModelTest {

    private val template = ReminderTemplate(activity = "Caminar", time = "08:00 AM", frequency = "Diario")
    private val viewModel = ReminderViewModel()

    @Test
    fun `initial state is null before start`() {
        assertNull(viewModel.state.value)
    }

    @Test
    fun `start initializes state with template details and Intro step`() {
        viewModel.start(template)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals("Caminar", state?.activity)
        assertEquals("08:00 AM", state?.time)
        assertEquals("Diario", state?.frequency)
        assertEquals(ReminderStep.Intro, state?.step)
    }

    @Test
    fun `StartNewReminderClicked moves to SelectType step`() {
        viewModel.start(template)
        viewModel.onAction(ReminderAction.StartNewReminderClicked)
        assertEquals(ReminderStep.SelectType, viewModel.state.value?.step)
    }

    @Test
    fun `TypeNextClicked moves to SelectSchedule step`() {
        viewModel.start(template)
        viewModel.onAction(ReminderAction.StartNewReminderClicked)
        viewModel.onAction(ReminderAction.TypeNextClicked)
        assertEquals(ReminderStep.SelectSchedule, viewModel.state.value?.step)
    }

    @Test
    fun `ScheduleNextClicked moves to SelectDetails step`() {
        viewModel.start(template)
        viewModel.onAction(ReminderAction.StartNewReminderClicked)
        viewModel.onAction(ReminderAction.TypeNextClicked)
        viewModel.onAction(ReminderAction.ScheduleNextClicked)
        assertEquals(ReminderStep.SelectDetails, viewModel.state.value?.step)
    }

    @Test
    fun `DetailsNextClicked moves to ReviewSummary step`() {
        viewModel.start(template)
        viewModel.onAction(ReminderAction.StartNewReminderClicked)
        viewModel.onAction(ReminderAction.TypeNextClicked)
        viewModel.onAction(ReminderAction.ScheduleNextClicked)
        viewModel.onAction(ReminderAction.DetailsNextClicked)
        assertEquals(ReminderStep.ReviewSummary, viewModel.state.value?.step)
    }

    @Test
    fun `SaveReminderClicked moves to Saved step`() {
        viewModel.start(template)
        viewModel.onAction(ReminderAction.StartNewReminderClicked)
        viewModel.onAction(ReminderAction.TypeNextClicked)
        viewModel.onAction(ReminderAction.ScheduleNextClicked)
        viewModel.onAction(ReminderAction.DetailsNextClicked)
        viewModel.onAction(ReminderAction.SaveReminderClicked)
        assertEquals(ReminderStep.Saved, viewModel.state.value?.step)
    }

    @Test
    fun `BackClicked moves back through all steps`() {
        viewModel.start(template)
        
        // Navigate forward
        viewModel.onAction(ReminderAction.StartNewReminderClicked)
        viewModel.onAction(ReminderAction.TypeNextClicked)
        viewModel.onAction(ReminderAction.ScheduleNextClicked)
        viewModel.onAction(ReminderAction.DetailsNextClicked)
        assertEquals(ReminderStep.ReviewSummary, viewModel.state.value?.step)
        
        // Navigate backward
        viewModel.onAction(ReminderAction.BackClicked)
        assertEquals(ReminderStep.SelectDetails, viewModel.state.value?.step)
        
        viewModel.onAction(ReminderAction.BackClicked)
        assertEquals(ReminderStep.SelectSchedule, viewModel.state.value?.step)
        
        viewModel.onAction(ReminderAction.BackClicked)
        assertEquals(ReminderStep.SelectType, viewModel.state.value?.step)
        
        viewModel.onAction(ReminderAction.BackClicked)
        assertEquals(ReminderStep.Intro, viewModel.state.value?.step)
        
        val event = viewModel.onAction(ReminderAction.BackClicked)
        assertEquals(ReminderEvent.ExitTask, event)
    }

    @Test
    fun `CancelClicked returns ExitTask event`() {
        viewModel.start(template)
        val event = viewModel.onAction(ReminderAction.CancelClicked)
        assertEquals(ReminderEvent.ExitTask, event)
    }
}
