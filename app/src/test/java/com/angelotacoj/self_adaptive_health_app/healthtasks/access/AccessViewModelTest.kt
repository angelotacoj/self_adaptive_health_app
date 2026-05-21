package com.angelotacoj.self_adaptive_health_app.healthtasks.access

import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccessViewModelTest {

    private val credentials = AccessCredentials(userCode = "USER123", simulatedPin = "1234")
    private val viewModel = AccessViewModel()

    @Test
    fun `initial state is null before start`() {
        assertNull(viewModel.state.value)
    }

    @Test
    fun `start initializes state with credentials and Intro step`() {
        viewModel.start(credentials)
        val state = viewModel.state.value
        assertNotNull(state)
        assertEquals(credentials, state?.credentials)
        assertEquals(AccessStep.Intro, state?.step)
    }

    @Test
    fun `StartClicked moves to Code step`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        assertEquals(AccessStep.Code, viewModel.state.value?.step)
    }

    @Test
    fun `UserCodeChanged updates state and uppercases value`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("user123"))
        assertEquals("USER123", viewModel.state.value?.userCode)
    }

    @Test
    fun `empty user code returns FieldError event`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged(""))
        
        val event = viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        
        assertEquals(AccessEvent.FieldError("user_code", "empty"), event)
        assertEquals("Ingrese el código de usuario mostrado en pantalla.", viewModel.state.value?.errorMessage)
    }

    @Test
    fun `wrong user code returns FieldError event`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("WRONG"))
        
        val event = viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        
        assertEquals(AccessEvent.FieldError("user_code", "invalid"), event)
    }

    @Test
    fun `correct user code moves to Pin step`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("USER123"))
        
        val event = viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        
        assertNull(event)
        assertEquals(AccessStep.Pin, viewModel.state.value?.step)
    }

    @Test
    fun `SimulatedPinChanged updates state and limits to 4 digits`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("USER123"))
        viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        
        viewModel.onAction(AccessAction.SimulatedPinChanged("12345"))
        
        assertEquals("1234", viewModel.state.value?.simulatedPin)
    }

    @Test
    fun `empty pin returns FieldError event`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("USER123"))
        viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        viewModel.onAction(AccessAction.SimulatedPinChanged(""))
        
        val event = viewModel.onAction(AccessAction.ValidateAccessClicked)
        
        assertEquals(AccessEvent.FieldError("simulated_pin", "empty"), event)
    }

    @Test
    fun `wrong pin returns FieldError event`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("USER123"))
        viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        viewModel.onAction(AccessAction.SimulatedPinChanged("0000"))
        
        val event = viewModel.onAction(AccessAction.ValidateAccessClicked)
        
        assertEquals(AccessEvent.FieldError("access_credentials", "invalid"), event)
    }

    @Test
    fun `correct credentials return null event and clear errors`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        viewModel.onAction(AccessAction.UserCodeChanged("USER123"))
        viewModel.onAction(AccessAction.ContinueFromCodeClicked)
        viewModel.onAction(AccessAction.SimulatedPinChanged("1234"))
        
        val event = viewModel.onAction(AccessAction.ValidateAccessClicked)
        
        assertNull(event)
        assertNull(viewModel.state.value?.errorMessage)
    }

    @Test
    fun `AccessValidated moves to Validation step`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.AccessValidated)
        assertEquals(AccessStep.Validation, viewModel.state.value?.step)
    }

    @Test
    fun `BackClicked from Code moves back to Intro`() {
        viewModel.start(credentials)
        viewModel.onAction(AccessAction.StartClicked)
        assertEquals(AccessStep.Code, viewModel.state.value?.step)
        
        viewModel.onAction(AccessAction.BackClicked)
        assertEquals(AccessStep.Intro, viewModel.state.value?.step)
    }

    @Test
    fun `CancelClicked returns ExitTask event`() {
        viewModel.start(credentials)
        val event = viewModel.onAction(AccessAction.CancelClicked)
        assertEquals(AccessEvent.ExitTask, event)
    }
}
