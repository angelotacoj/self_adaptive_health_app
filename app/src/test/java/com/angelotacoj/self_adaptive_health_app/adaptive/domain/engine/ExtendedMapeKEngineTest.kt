package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

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

class ExtendedMapeKEngineTest {
    @Test
    fun ar02_prolongedTime_appliesTextScaleContextualHelpAndVisualFeedback() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())

        val result = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME)

        val applied = result as AdaptationEngineResult.Applied
        assertEquals(AdaptationRuleId.AR02, applied.plan.ruleId)
        assertEquals(ValidationType.NON_INTRUSIVE, applied.plan.validationType)
        assertEquals(listOf(InferredDifficulty.DI02_VISUAL_OR_COGNITIVE), applied.plan.difficulties)
        assertEquals(
            listOf(
                UiModification.UIM01_TEXT_SIZE,
                UiModification.UIM06_CONTEXTUAL_HELP,
                UiModification.UIM09_VISUAL_FEEDBACK
            ),
            applied.plan.modifications
        )
        assertEquals(1.25f, applied.state.textScale)
        assertTrue(applied.state.contextualHelpVisible)
        assertEquals(
            "Siguiente paso sugerido: revise la información y toque el botón para continuar.",
            applied.state.contextualHelpMessage
        )
        assertNotNull(applied.state.lastAppliedAdaptation)
        assertTrue(applied.state.undoMessageVisible)
    }

    @Test
    fun ar02_undoRestoresTextScale() {
        val repository = InMemoryKnowledgeRepository()
        val engine = ExtendedMapeKEngine(repository)
        val applied = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME) as AdaptationEngineResult.Applied

        val undone = engine.undo(applied.plan, applied.state)

        assertEquals(1.0f, undone.textScale)
        assertFalse(undone.undoMessageVisible)
        assertTrue(undone.contextualHelpVisible)
        assertEquals("Entendido. No volveré a mostrar esta sugerencia durante esta tarea.", undone.contextualHelpMessage)
        assertTrue(repository.snapshot(TaskId.T1_ACCESS, ScreenId.ACCESS_CODE).rejectedRulesForTask.contains(AdaptationRuleId.AR02))
    }

    @Test
    fun ar05_helpRequestAppliesDirectContextualHelp() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())

        val result = engine.processAccessEvent(AdaptiveInteractionEventType.HELP_REQUESTED)

        val applied = result as AdaptationEngineResult.Applied
        assertEquals(AdaptationRuleId.AR05, applied.plan.ruleId)
        assertEquals(ValidationType.DIRECT, applied.plan.validationType)
        assertEquals(listOf(UiModification.UIM06_CONTEXTUAL_HELP), applied.plan.modifications)
        assertTrue(applied.state.contextualHelpVisible)
        assertFalse(applied.state.undoMessageVisible)
        assertEquals(null, applied.state.lastAppliedAdaptation)
    }

    @Test
    fun helpRequestAfterProlongedTimeUsesAr05AndDoesNotShowUndoCard() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())
        val ar02 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME) as AdaptationEngineResult.Applied

        val result = engine.processAccessEvent(
            eventType = AdaptiveInteractionEventType.HELP_REQUESTED,
            currentState = ar02.state.copy(undoMessageVisible = false)
        )

        val applied = result as AdaptationEngineResult.Applied
        assertEquals(AdaptationRuleId.AR05, applied.plan.ruleId)
        assertTrue(applied.state.contextualHelpVisible)
        assertFalse(applied.state.undoMessageVisible)
    }

    @Test
    fun helpRequestStillWorksAfterRejectingProlongedTimeAdaptation() {
        val repository = InMemoryKnowledgeRepository()
        val engine = ExtendedMapeKEngine(repository)
        val ar02 = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME) as AdaptationEngineResult.Applied
        val undone = engine.undo(ar02.plan, ar02.state)

        val result = engine.processAccessEvent(
            eventType = AdaptiveInteractionEventType.HELP_REQUESTED,
            currentState = undone.copy(contextualHelpVisible = false)
        )

        val applied = result as AdaptationEngineResult.Applied
        assertEquals(AdaptationRuleId.AR05, applied.plan.ruleId)
        assertTrue(applied.state.contextualHelpVisible)
        assertFalse(applied.state.undoMessageVisible)
    }

    @Test
    fun ar06_fieldErrorAppliesRecoveryFeedbackAndSafeExit() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())

        val result = engine.processAccessEvent(
            eventType = AdaptiveInteractionEventType.FIELD_ERROR,
            fieldErrorCount = 1
        )

        val applied = result as AdaptationEngineResult.Applied
        assertEquals(AdaptationRuleId.AR06, applied.plan.ruleId)
        assertEquals(ValidationType.DIRECT, applied.plan.validationType)
        assertEquals(listOf(UiModification.UIM09_VISUAL_FEEDBACK, UiModification.UIM10_SAFE_EXIT), applied.plan.modifications)
        assertTrue(applied.state.contextualHelpVisible)
        assertTrue(applied.state.safeExitEnabled)
    }

    @Test
    fun ar08_sensitiveActionRequiresExplicitValidation() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())

        val result = engine.processAccessEvent(AdaptiveInteractionEventType.SENSITIVE_ACTION)

        val pending = result as AdaptationEngineResult.RequiresUserValidation
        assertEquals(AdaptationRuleId.AR08, pending.plan.ruleId)
        assertEquals(ValidationType.EXPLICIT, pending.plan.validationType)
        assertEquals(
            listOf(UiModification.UIM08_REINFORCED_CONFIRMATION, UiModification.UIM10_SAFE_EXIT),
            pending.plan.modifications
        )
        assertEquals(AdaptationRuleId.AR08, pending.state.pendingAdaptation?.ruleId)
    }

    @Test
    fun ar03_confirmationPauseRequiresExplicitValidation() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())

        val result = engine.processAccessEvent(AdaptiveInteractionEventType.CONFIRMATION_PAUSE)

        val pending = result as AdaptationEngineResult.RequiresUserValidation
        assertEquals(AdaptationRuleId.AR03, pending.plan.ruleId)
        assertEquals(ValidationType.EXPLICIT, pending.plan.validationType)
        assertEquals(
            listOf(UiModification.UIM08_REINFORCED_CONFIRMATION, UiModification.UIM10_SAFE_EXIT),
            pending.plan.modifications
        )
    }

    @Test
    fun ar04_backtrackingRequiresSuggestedGuidedNavigationAfterTwoBackEvents() {
        val engine = ExtendedMapeKEngine(InMemoryKnowledgeRepository())

        engine.processAccessEvent(AdaptiveInteractionEventType.BACK_PRESSED)
        val result = engine.processAccessEvent(AdaptiveInteractionEventType.BACK_PRESSED, backCountInTask = 2)

        val pending = result as AdaptationEngineResult.RequiresUserValidation
        assertEquals(AdaptationRuleId.AR04, pending.plan.ruleId)
        assertEquals(ValidationType.SUGGESTED, pending.plan.validationType)
        assertEquals(
            listOf(UiModification.UIM07_GUIDED_NAVIGATION, UiModification.UIM09_VISUAL_FEEDBACK),
            pending.plan.modifications
        )
    }

    @Test
    fun rejectedRuleIsSuppressedByKnowledgeBase() {
        val repository = InMemoryKnowledgeRepository()
        repository.rememberRejected(TaskId.T1_ACCESS, AdaptationRuleId.AR02)
        val engine = ExtendedMapeKEngine(repository)

        val result = engine.processAccessEvent(AdaptiveInteractionEventType.PROLONGED_TIME)

        assertEquals(AdaptationEngineResult.NoAdaptation, result)
    }
}

private fun ExtendedMapeKEngine.processAccessEvent(
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
