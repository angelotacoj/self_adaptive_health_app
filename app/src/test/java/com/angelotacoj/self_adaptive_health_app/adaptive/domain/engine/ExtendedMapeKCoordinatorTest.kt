package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEvent
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.InferredDifficulty
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.InMemoryKnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtendedMapeKCoordinatorTest {

    private class TestKnowledgeRepository : InMemoryKnowledgeRepository() {
        fun clearSuggestions() {
            suggestedCountByTask.clear()
        }
        fun clearLastRejection() {
            lastRejectionAt = null
        }
    }

    @Test
    fun ar01_prolongedTime_escalatesProgressively() {
        val repository = TestKnowledgeRepository()
        val engine = ExtendedMapeKCoordinator(repository)

        // Trigger 1: Should apply Level 1 (Light Support)
        var state = AdaptiveUiState()
        val result1 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = state)
        val applied1 = result1 as AdaptationEngineResult.Applied
        val state1 = applied1.state
        assertEquals(AdaptationRuleId.AR01_TIME_ON_SCREEN, applied1.plan.ruleId)
        assertEquals(AdaptationLevel.LEVEL_1_LIGHT_SUPPORT, applied1.plan.targetLevel)
        assertEquals(1.15f, state1.textScale)
        assertFalse(state1.highContrast)
        assertFalse(state1.enlargedTouchTargets)
        assertFalse(state1.increasedSpacing)

        // Trigger 2: Should escalate to Level 2 (Moderate Support)
        val result2 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = state1)
        val applied2 = result2 as AdaptationEngineResult.Applied
        val state2 = applied2.state
        assertEquals(AdaptationLevel.LEVEL_2_MODERATE_SUPPORT, applied2.plan.targetLevel)
        assertEquals(1.25f, state2.textScale)
        assertTrue(state2.highContrast)
        assertFalse(state2.enlargedTouchTargets)
        assertFalse(state2.increasedSpacing)

        // Trigger 3: Should escalate to Level 3 (High Support)
        val result3 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = state2)
        val applied3 = result3 as AdaptationEngineResult.Applied
        val state3 = applied3.state
        assertEquals(AdaptationLevel.LEVEL_3_HIGH_SUPPORT, applied3.plan.targetLevel)
        assertEquals(1.35f, state3.textScale)
        assertTrue(state3.highContrast)
        assertTrue(state3.enlargedTouchTargets)
        assertTrue(state3.increasedSpacing)
    }

    @Test
    fun ar01_undoRestoresPreviousLevel() {
        val repository = TestKnowledgeRepository()
        val engine = ExtendedMapeKCoordinator(repository)

        // Escalate to Level 1
        val res1 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = AdaptiveUiState()) as AdaptationEngineResult.Applied
        val state1 = res1.state
        
        // Escalate to Level 2
        val res2 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = state1) as AdaptationEngineResult.Applied
        val state2 = res2.state
        
        // Undo Level 2: Should revert back to Level 1 state
        val undone = engine.undo(res2.plan, state2)
        assertEquals(AdaptationLevel.LEVEL_1_LIGHT_SUPPORT, undone.uim01Level)
        assertEquals(1.15f, undone.textScale)
        assertFalse(undone.highContrast)
        
        // And Level 2 should now be recorded as rejected
        val snapshot = repository.snapshot(TaskId.T1_ACCESS, ScreenId.ACCESS_CODE)
        assertTrue(snapshot.rejectedRuleLevelsForTask.contains(Pair(AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationLevel.LEVEL_2_MODERATE_SUPPORT)))
    }

    @Test
    fun levelSpecificRejection_skipsRejectedLevel() {
        val repository = TestKnowledgeRepository()
        // Reject Level 1 in advance
        repository.rememberRejected(TaskId.T1_ACCESS, AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationLevel.LEVEL_1_LIGHT_SUPPORT)
        // Reset lastRejectionAt to bypass the Safety Controller's "recent rejection" guard (limit is 20s)
        repository.clearLastRejection()
        
        val engine = ExtendedMapeKCoordinator(repository)

        // Because Level 1 is rejected, it should skip directly to Level 2!
        val result = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = AdaptiveUiState())
        val applied = result as AdaptationEngineResult.Applied
        val state = applied.state
        assertEquals(AdaptationRuleId.AR01_TIME_ON_SCREEN, applied.plan.ruleId)
        assertEquals(AdaptationLevel.LEVEL_2_MODERATE_SUPPORT, applied.plan.targetLevel)
        assertEquals(1.25f, state.textScale)
    }

    @Test
    fun allLevelsRejected_suppressesRuleCompletely() {
        val repository = TestKnowledgeRepository()
        repository.rememberRejected(TaskId.T1_ACCESS, AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationLevel.LEVEL_1_LIGHT_SUPPORT)
        repository.rememberRejected(TaskId.T1_ACCESS, AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationLevel.LEVEL_2_MODERATE_SUPPORT)
        repository.rememberRejected(TaskId.T1_ACCESS, AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationLevel.LEVEL_3_HIGH_SUPPORT)

        val engine = ExtendedMapeKCoordinator(repository)
        val result = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME, currentState = AdaptiveUiState())

        assertTrue(result is AdaptationEngineResult.Suppressed)
        val suppressed = result as AdaptationEngineResult.Suppressed
        assertEquals("MAX_LEVEL_REACHED", suppressed.reason)
    }

    @Test
    fun ar03_helpRequestAppliesDirectContextualHelp() {
        val engine = ExtendedMapeKCoordinator(InMemoryKnowledgeRepository())
        val result = engine.processAccessEvent(AdaptiveInteractionEventType.HELP_REQUESTED)
        val applied = result as AdaptationEngineResult.Applied
        
        assertEquals(AdaptationRuleId.AR03_HELP_REQUEST, applied.plan.ruleId)
        assertEquals(ValidationType.DIRECT, applied.plan.validationType)
        assertEquals(AdaptationLevel.LEVEL_1_LIGHT_SUPPORT, applied.plan.targetLevel)
        assertTrue(applied.state.showIconLabels)
    }

    @Test
    fun ar04_fieldErrorAppliesRecoveryFeedback() {
        val engine = ExtendedMapeKCoordinator(InMemoryKnowledgeRepository())
        val result = engine.processAccessEvent(
            eventType = AdaptiveInteractionEventType.FIELD_ERROR,
            fieldErrorCount = 1
        )
        val applied = result as AdaptationEngineResult.Applied
        assertEquals(AdaptationRuleId.AR04_FIELD_ERROR, applied.plan.ruleId)
        assertEquals(AdaptationLevel.LEVEL_1_LIGHT_SUPPORT, applied.plan.targetLevel)
        assertTrue(applied.state.contextualHelpVisible)
    }

    @Test
    fun ar05_confirmationPauseRequiresExplicitValidation() {
        val engine = ExtendedMapeKCoordinator(InMemoryKnowledgeRepository())
        val reviewSummary = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary(
            title = "Datos simulados",
            details = mapOf("Test" to "Value")
        )
        val result = engine.process(
            event = AdaptiveInteractionEvent(
                taskId = TaskId.T1_ACCESS,
                screenId = ScreenId.ACCESS_CODE,
                eventType = AdaptiveInteractionEventType.SENSITIVE_ACTION,
                reviewSummary = reviewSummary
            ),
            taskState = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.TaskInteractionState(
                taskId = TaskId.T1_ACCESS,
                screenId = ScreenId.ACCESS_CODE,
                screenEnteredAt = 0L,
                successfulActionAt = 0L,
                backCountInTask = 0,
                fieldErrorCount = 0
            ),
            currentState = AdaptiveUiState()
        )

        val pending = result as AdaptationEngineResult.RequiresUserValidation
        assertEquals(AdaptationRuleId.AR05_CONFIRMATION_PAUSE, pending.plan.ruleId)
        assertEquals(ValidationType.EXPLICIT, pending.plan.validationType)
        assertEquals(AdaptationLevel.LEVEL_1_LIGHT_SUPPORT, pending.plan.targetLevel)
    }
}

private fun ExtendedMapeKCoordinator.processAccessEvent(
    eventType: AdaptiveInteractionEventType,
    backCountInTask: Int = 0,
    fieldErrorCount: Int = 0,
    currentState: AdaptiveUiState = AdaptiveUiState()
): AdaptationEngineResult {
    return process(
        event = AdaptiveInteractionEvent(
            taskId = TaskId.T1_ACCESS,
            screenId = ScreenId.ACCESS_CODE,
            eventType = eventType
        ),
        taskState = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.TaskInteractionState(
            taskId = TaskId.T1_ACCESS,
            screenId = ScreenId.ACCESS_CODE,
            screenEnteredAt = 0L,
            successfulActionAt = 0L,
            backCountInTask = backCountInTask,
            fieldErrorCount = fieldErrorCount
        ),
        currentState = currentState
    )
}
