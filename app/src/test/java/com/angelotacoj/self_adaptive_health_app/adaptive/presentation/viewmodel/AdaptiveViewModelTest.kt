package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.viewmodel

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.ExtendedMapeKCoordinator
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.InMemoryKnowledgeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveViewModelTest {
    private fun viewModel(): AdaptiveViewModel {
        val repository = InMemoryKnowledgeRepository()
        return AdaptiveViewModel(ExtendedMapeKCoordinator(repository), repository)
    }

    @Test
    fun selfAdaptiveTaskStartsFromBaselineWithoutAcceptedPreference() {
        val viewModel = viewModel()

        viewModel.resetTemporaryStateForTask(isAdaptive = true)

        val state = viewModel.uiState.value
        assertTrue(state.isAdaptiveMode)
        assertEquals(1.0f, state.textScale)
        assertFalse(state.highContrast)
        assertFalse(state.enlargedTouchTargets)
        assertFalse(state.increasedSpacing)
    }

    @Test
    fun temporaryAdaptationsResetBetweenTasks() {
        val viewModel = viewModel()
        viewModel.resetTemporaryStateForTask(isAdaptive = true)
        viewModel.rememberAcceptedPersistentPreference(UiModification.UIM01_TEXT_SIZE)

        viewModel.resetState()
        viewModel.resetTemporaryStateForTask(isAdaptive = true)

        assertEquals(1.0f, viewModel.uiState.value.textScale)
    }

    @Test
    fun acceptedFontSizePreferencePersistsToLaterSelfAdaptiveTask() {
        val viewModel = viewModel()
        viewModel.resetTemporaryStateForTask(isAdaptive = true)
        viewModel.rememberAcceptedPersistentPreference(UiModification.UIM01_TEXT_SIZE)

        viewModel.resetTemporaryStateForTask(isAdaptive = true)

        assertEquals(1.25f, viewModel.uiState.value.textScale)
    }

    @Test
    fun staticModeNeverAppliesAdaptivePreferences() {
        val viewModel = viewModel()
        viewModel.resetTemporaryStateForTask(isAdaptive = true)
        viewModel.rememberAcceptedPersistentPreference(UiModification.UIM01_TEXT_SIZE)

        viewModel.resetTemporaryStateForTask(isAdaptive = false)

        val state = viewModel.uiState.value
        assertFalse(state.isAdaptiveMode)
        assertEquals(1.0f, state.textScale)
    }
}
