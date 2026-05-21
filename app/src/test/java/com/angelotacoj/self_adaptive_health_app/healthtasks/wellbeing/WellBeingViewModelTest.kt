package com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing

import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WellBeingViewModelTest {

    private val record = WellBeingRecord(label = "Nivel de energía", value = 5)
    private val viewModel = WellBeingViewModel()

    @Test
    fun `initial state is null before start`() {
        assertNull(viewModel.state.value)
    }

    @Test
    fun `start initializes state with record details and Intro step`() {
        viewModel.start(record)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals("Nivel de energía", state?.label)
        assertEquals(5, state?.suggestedValue)
        assertEquals("", state?.valueText)
        assertEquals(WellBeingStep.Intro, state?.step)
    }

    @Test
    fun `StartFormClicked moves to Form step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        assertEquals(WellBeingStep.Form, viewModel.state.value?.step)
    }

    @Test
    fun `ValueChanged updates state and filters non-digits`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.ValueChanged("10a"))
        assertEquals("10", viewModel.state.value?.valueText)
    }

    @Test
    fun `ValidateClicked with invalid value shows error and increments fieldErrorCount`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.ValueChanged("15")) // Invalid (out of 1-10 range)
        
        viewModel.onAction(WellBeingAction.ValidateClicked)
        
        val state = viewModel.state.value
        assertEquals("Ingrese un valor ficticio del 1 al 10.", state?.errorMessage)
        assertEquals(1, state?.fieldErrorCount)
        assertEquals(WellBeingStep.Form, state?.step)
    }

    @Test
    fun `ValidateClicked with valid value moves to Validation step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.ValueChanged("8"))
        
        viewModel.onAction(WellBeingAction.ValidateClicked)
        
        val state = viewModel.state.value
        assertNull(state?.errorMessage)
        assertEquals(WellBeingStep.Validation, state?.step)
    }

    @Test
    fun `ContinueToReviewClicked moves to Review step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.ValidateClicked)
        viewModel.onAction(WellBeingAction.ContinueToReviewClicked)
        
        assertEquals(WellBeingStep.Review, viewModel.state.value?.step)
    }

    @Test
    fun `SaveClicked moves to Success step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.ValidateClicked)
        viewModel.onAction(WellBeingAction.ContinueToReviewClicked)
        viewModel.onAction(WellBeingAction.SaveClicked)
        
        assertEquals(WellBeingStep.Success, viewModel.state.value?.step)
    }

    @Test
    fun `EditClicked moves back to Form step from Review`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.ValidateClicked)
        viewModel.onAction(WellBeingAction.ContinueToReviewClicked)
        
        viewModel.onAction(WellBeingAction.EditClicked)
        
        assertEquals(WellBeingStep.Form, viewModel.state.value?.step)
    }

    @Test
    fun `BackClicked from Form moves back to Intro`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        assertEquals(WellBeingStep.Form, viewModel.state.value?.step)
        
        viewModel.onAction(WellBeingAction.BackClicked)
        assertEquals(WellBeingStep.Intro, viewModel.state.value?.step)
    }

    @Test
    fun `CancelClicked returns ExitTask event`() {
        viewModel.start(record)
        val event = viewModel.onAction(WellBeingAction.CancelClicked)
        assertEquals(WellBeingEvent.ExitTask, event)
    }
}
