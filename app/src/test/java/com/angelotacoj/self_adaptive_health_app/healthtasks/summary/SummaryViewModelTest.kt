package com.angelotacoj.self_adaptive_health_app.healthtasks.summary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryViewModelTest {

    private val taskOutputs = mapOf(
        "T1_ACCESS" to "Código verificado.",
        "T2_APPOINTMENT" to "Cita confirmada.",
        "T3_WELL_BEING" to "Nivel de energía: 8",
        "T4_REMINDER" to "Recordatorio configurado."
    )
    private val viewModel = SummaryViewModel()

    @Test
    fun `initial state is null before start`() {
        assertNull(viewModel.state.value)
    }

    @Test
    fun `start initializes state with taskOutputs and Intro step`() {
        viewModel.start(taskOutputs)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals(taskOutputs, state?.taskOutputs)
        assertEquals(SummaryStep.Intro, state?.step)
    }

    @Test
    fun `StartReviewClicked moves to Details step`() {
        viewModel.start(taskOutputs)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        assertEquals(SummaryStep.Details, viewModel.state.value?.step)
    }

    @Test
    fun `SaveInformationClicked moves to ReinforcedConfirmation step`() {
        viewModel.start(taskOutputs)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        assertEquals(SummaryStep.ReinforcedConfirmation, viewModel.state.value?.step)
    }

    @Test
    fun `ConfirmClicked moves to Final step with Confirmed result`() {
        viewModel.start(taskOutputs)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        viewModel.onAction(SummaryAction.ConfirmClicked)
        
        val state = viewModel.state.value
        assertEquals(SummaryStep.Final, state?.step)
        assertEquals(SummaryResult.Confirmed, state?.result)
    }

    @Test
    fun `EditClicked moves back to Details step and clears result`() {
        viewModel.start(taskOutputs)
        viewModel.onAction(SummaryAction.StartReviewClicked)
        viewModel.onAction(SummaryAction.SaveInformationClicked)
        viewModel.onAction(SummaryAction.EditClicked)
        
        val state = viewModel.state.value
        assertEquals(SummaryStep.Details, state?.step)
        assertNull(state?.result)
        assertEquals("El participante seleccionó Editar. No se modificaron datos reales.", state?.editNote)
    }

    @Test
    fun `CancelClicked moves to Final step with Cancelled result`() {
        viewModel.start(taskOutputs)
        viewModel.onAction(SummaryAction.CancelClicked)
        
        val state = viewModel.state.value
        assertEquals(SummaryStep.Final, state?.step)
        assertEquals(SummaryResult.Cancelled, state?.result)
    }

    @Test
    fun `BackClicked moves back through steps`() {
        viewModel.start(taskOutputs)
        
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

    @Test
    fun `start with empty taskOutputs represents fallback behavior scenario`() {
        viewModel.start(emptyMap())
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals(emptyMap<String, String>(), state?.taskOutputs)
    }
}
