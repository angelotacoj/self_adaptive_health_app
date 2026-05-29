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
    fun `start initializes state and Intro step`() {
        viewModel.start(record)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals(WellBeingStep.Intro, state?.step)
    }

    @Test
    fun `StartFormClicked moves to Form step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        assertEquals(WellBeingStep.Form, viewModel.state.value?.step)
    }

    @Test
    fun `EnergyLevelChanged updates state and filters non-digits`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.EnergyLevelChanged("10a"))
        assertEquals("10", viewModel.state.value?.energyLevel)
    }

    @Test
    fun `ValidateAndReviewClicked with invalid energy shows error and increments fieldErrorCount`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.EnergyLevelChanged("15")) // Invalid (out of 1-10 range)
        
        viewModel.onAction(WellBeingAction.ValidateAndReviewClicked)
        
        val state = viewModel.state.value
        assertEquals("Ingrese un número entre 1 y 10 para el nivel de energía simulado.", state?.errorMessage)
        assertEquals(1, state?.fieldErrorCount)
        assertEquals(WellBeingStep.Form, state?.step)
    }

    @Test
    fun `ValidateAndReviewClicked with valid values moves to Review step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.EnergyLevelChanged("8"))
        viewModel.onAction(WellBeingAction.MoodSelected("Feliz"))
        
        viewModel.onAction(WellBeingAction.ValidateAndReviewClicked)
        
        val state = viewModel.state.value
        assertNull(state?.errorMessage)
        assertEquals(WellBeingStep.Review, state?.step)
    }

    @Test
    fun `SaveClicked moves to Success step`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.EnergyLevelChanged("8"))
        viewModel.onAction(WellBeingAction.MoodSelected("Feliz"))
        viewModel.onAction(WellBeingAction.ValidateAndReviewClicked)
        viewModel.onAction(WellBeingAction.SaveClicked)
        
        assertEquals(WellBeingStep.Success, viewModel.state.value?.step)
    }

    @Test
    fun `EditClicked moves back to Form step from Review`() {
        viewModel.start(record)
        viewModel.onAction(WellBeingAction.StartFormClicked)
        viewModel.onAction(WellBeingAction.EnergyLevelChanged("8"))
        viewModel.onAction(WellBeingAction.MoodSelected("Feliz"))
        viewModel.onAction(WellBeingAction.ValidateAndReviewClicked)
        
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
