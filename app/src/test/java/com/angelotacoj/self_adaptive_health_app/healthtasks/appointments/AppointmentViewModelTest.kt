package com.angelotacoj.self_adaptive_health_app.healthtasks.appointments

import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentViewModelTest {

    private val target = Appointment("Cita Dental", "20/05/2026", "10:00 AM", "Traer carnet", "Dr. A", "Odontología", "Piso 1", "Nada", "Carnet", "Ninguna")
    private val options = listOf(
        Appointment("Cita Dental", "20/05/2026", "10:00 AM", "Traer carnet", "Dr. A", "Odontología", "Piso 1", "Nada", "Carnet", "Ninguna"),
        Appointment("Cita Médica", "21/05/2026", "11:00 AM", "Ayunas", "Dr. B", "Medicina General", "Piso 2", "Ayunas 8h", "DNI", "Rampa")
    )
    private val viewModel = AppointmentViewModel()

    @Test
    fun `initial state is null before start`() {
        assertNull(viewModel.state.value)
    }

    @Test
    fun `start initializes state with target and options and Overview step`() {
        viewModel.start(target, options)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals(target, state?.targetAppointment)
        assertEquals(options, state?.appointmentOptions)
        assertEquals(AppointmentStep.Overview, state?.step)
    }

    @Test
    fun `StartListClicked moves to List step`() {
        viewModel.start(target, options)
        viewModel.onAction(AppointmentAction.StartListClicked)
        assertEquals(AppointmentStep.List, viewModel.state.value?.step)
    }

    @Test
    fun `AppointmentSelected moves to Detail step and sets selectedAppointment`() {
        viewModel.start(target, options)
        viewModel.onAction(AppointmentAction.StartListClicked)
        
        val selected = options[0]
        viewModel.onAction(AppointmentAction.AppointmentSelected(selected))
        
        val state = viewModel.state.value
        assertEquals(AppointmentStep.Detail, state?.step)
        assertEquals(selected, state?.selectedAppointment)
    }

    @Test
    fun `ContinueFromDetailClicked moves to Confirmation step`() {
        viewModel.start(target, options)
        viewModel.onAction(AppointmentAction.StartListClicked)
        viewModel.onAction(AppointmentAction.AppointmentSelected(options[0]))
        viewModel.onAction(AppointmentAction.ContinueFromDetailClicked)
        
        assertEquals(AppointmentStep.Confirmation, viewModel.state.value?.step)
    }

    @Test
    fun `ConfirmFoundClicked moves to Completed step`() {
        viewModel.start(target, options)
        viewModel.onAction(AppointmentAction.StartListClicked)
        viewModel.onAction(AppointmentAction.AppointmentSelected(options[0]))
        viewModel.onAction(AppointmentAction.ContinueFromDetailClicked)
        viewModel.onAction(AppointmentAction.ConfirmFoundClicked)
        
        assertEquals(AppointmentStep.Completed, viewModel.state.value?.step)
    }

    @Test
    fun `ReviewAgainClicked moves back to Detail step`() {
        viewModel.start(target, options)
        viewModel.onAction(AppointmentAction.StartListClicked)
        viewModel.onAction(AppointmentAction.AppointmentSelected(options[0]))
        viewModel.onAction(AppointmentAction.ContinueFromDetailClicked)
        
        viewModel.onAction(AppointmentAction.ReviewAgainClicked)
        
        assertEquals(AppointmentStep.Detail, viewModel.state.value?.step)
    }

    @Test
    fun `BackClicked moves back through steps`() {
        viewModel.start(target, options)
        
        viewModel.onAction(AppointmentAction.StartListClicked)
        viewModel.onAction(AppointmentAction.AppointmentSelected(options[0]))
        viewModel.onAction(AppointmentAction.ContinueFromDetailClicked)
        assertEquals(AppointmentStep.Confirmation, viewModel.state.value?.step)
        
        viewModel.onAction(AppointmentAction.BackClicked)
        assertEquals(AppointmentStep.Detail, viewModel.state.value?.step)
        
        viewModel.onAction(AppointmentAction.BackClicked)
        assertEquals(AppointmentStep.List, viewModel.state.value?.step)
        
        viewModel.onAction(AppointmentAction.BackClicked)
        assertEquals(AppointmentStep.Overview, viewModel.state.value?.step)
        
        val event = viewModel.onAction(AppointmentAction.BackClicked)
        assertEquals(AppointmentEvent.ExitTask, event)
    }

    @Test
    fun `CancelClicked returns ExitTask event`() {
        viewModel.start(target, options)
        val event = viewModel.onAction(AppointmentAction.CancelClicked)
        assertEquals(AppointmentEvent.ExitTask, event)
    }
}
