package com.angelotacoj.self_adaptive_health_app.healthtasks.home

import com.angelotacoj.self_adaptive_health_app.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = HomeViewModel()

    @Test
    fun `initial uiState is correct`() {
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showCancelSessionConfirmation)
        assertFalse(uiState.showSessionHelp)
    }

    @Test
    fun `AccessTaskClicked emits OpenAccess event`() = runTest {
        val results = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { results.add(it) }
        }

        viewModel.onAction(HomeAction.AccessTaskClicked)
        assertEquals(HomeEvent.OpenAccess, results.first())
    }

    @Test
    fun `HelpClicked updates uiState and emits HelpRequested event`() = runTest {
        val results = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { results.add(it) }
        }

        viewModel.onAction(HomeAction.HelpClicked)
        
        assertTrue(viewModel.uiState.value.showSessionHelp)
        assertEquals(HomeEvent.HelpRequested, results.first())
    }

    @Test
    fun `DismissHelpClicked updates uiState`() {
        viewModel.onAction(HomeAction.HelpClicked)
        assertTrue(viewModel.uiState.value.showSessionHelp)
        
        viewModel.onAction(HomeAction.DismissHelpClicked)
        assertFalse(viewModel.uiState.value.showSessionHelp)
    }

    @Test
    fun `CancelSessionClicked updates uiState`() {
        viewModel.onAction(HomeAction.CancelSessionClicked)
        assertTrue(viewModel.uiState.value.showCancelSessionConfirmation)
    }

    @Test
    fun `ConfirmCancelSessionClicked updates uiState and emits NavigateToSetup event`() = runTest {
        val results = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { results.add(it) }
        }

        viewModel.onAction(HomeAction.CancelSessionClicked)
        viewModel.onAction(HomeAction.ConfirmCancelSessionClicked)
        
        assertFalse(viewModel.uiState.value.showCancelSessionConfirmation)
        assertEquals(HomeEvent.NavigateToSetup, results.first())
    }

    @Test
    fun `DismissCancelSessionClicked updates uiState`() {
        viewModel.onAction(HomeAction.CancelSessionClicked)
        assertTrue(viewModel.uiState.value.showCancelSessionConfirmation)
        
        viewModel.onAction(HomeAction.DismissCancelSessionClicked)
        assertFalse(viewModel.uiState.value.showCancelSessionConfirmation)
    }
}
