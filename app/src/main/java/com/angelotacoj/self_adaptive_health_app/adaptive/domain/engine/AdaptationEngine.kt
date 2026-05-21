package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationPlan
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEvent
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AppliedAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ControlledAdaptationPlan
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.InferredDifficulty
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.KnowledgeSnapshot
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ObservedInteractionSignal
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.TaskInteractionState
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationResult
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.KnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.logging.MapeKLog
import java.util.UUID

sealed interface AdaptationEngineResult {
    data object NoAdaptation : AdaptationEngineResult
    data class Applied(val state: AdaptiveUiState, val plan: AdaptationPlan) : AdaptationEngineResult
    data class RequiresUserValidation(val state: AdaptiveUiState, val plan: AdaptationPlan) : AdaptationEngineResult
}

interface InteractionMonitor {
    fun recordEvent(event: AdaptiveInteractionEvent)
    fun evaluateSignals(taskState: TaskInteractionState): List<ObservedInteractionSignal>
    fun clearEvents(taskId: TaskId?)
}

interface AdaptationAnalyzer {
    fun inferDifficulties(
        signals: List<ObservedInteractionSignal>,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): List<InferredDifficulty>
}

interface AdaptationPlanner {
    fun createPlan(
        difficulties: List<InferredDifficulty>,
        signals: List<ObservedInteractionSignal>,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot,
        event: AdaptiveInteractionEvent
    ): AdaptationPlan?
}

interface AdaptationController {
    fun validatePlan(
        plan: AdaptationPlan,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): ControlledAdaptationPlan
}

interface AdaptationExecutor {
    fun apply(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState
    fun undo(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState
}

class InteractionMonitorImpl : InteractionMonitor {
    private val events = mutableListOf<AdaptiveInteractionEvent>()

    override fun recordEvent(event: AdaptiveInteractionEvent) {
        events += event
        MapeKLog.stage("MONITOR", "interaction event=${event.eventType} task=${event.taskId} screen=${event.screenId}")
    }

    override fun evaluateSignals(taskState: TaskInteractionState): List<ObservedInteractionSignal> {
        val taskEvents = events.filter { it.taskId == taskState.taskId }
        val screenEvents = taskEvents.filter { it.screenId == taskState.screenId }
        val signals = mutableListOf<ObservedInteractionSignal>()
        
        if (screenEvents.count { it.eventType == AdaptiveInteractionEventType.TOUCH_ERROR } == 2) {
            signals += ObservedInteractionSignal.OIS01_TOUCH_ERRORS
        }
        
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.PROLONGED_TIME }) signals += ObservedInteractionSignal.OIS02_PROLONGED_TIME
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.CONFIRMATION_PAUSE }) signals += ObservedInteractionSignal.OIS03_CONFIRMATION_PAUSE
        if (taskEvents.count { it.eventType == AdaptiveInteractionEventType.BACK_PRESSED } >= 2) signals += ObservedInteractionSignal.OIS04_BACKTRACKING
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.HELP_REQUESTED }) signals += ObservedInteractionSignal.OIS05_HELP_REQUEST
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.FIELD_ERROR } || taskState.fieldErrorCount > 0) signals += ObservedInteractionSignal.OIS06_FIELD_ERROR
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.ADAPTATION_REJECTED }) signals += ObservedInteractionSignal.OIS07_ADAPTATION_REJECTED
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.SENSITIVE_ACTION }) signals += ObservedInteractionSignal.OIS08_SENSITIVE_ACTION
        
        return signals
    }

    override fun clearEvents(taskId: TaskId?) {
        if (taskId == null) events.clear() else events.removeAll { it.taskId == taskId }
    }
}

class DifficultyAnalyzer : AdaptationAnalyzer {
    override fun inferDifficulties(
        signals: List<ObservedInteractionSignal>,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): List<InferredDifficulty> {
        return signals.mapNotNull {
            when (it) {
                ObservedInteractionSignal.OIS01_TOUCH_ERRORS -> InferredDifficulty.DI01_MOTOR_PRECISION
                ObservedInteractionSignal.OIS02_PROLONGED_TIME,
                ObservedInteractionSignal.OIS06_FIELD_ERROR -> InferredDifficulty.DI02_VISUAL_OR_COGNITIVE
                ObservedInteractionSignal.OIS04_BACKTRACKING -> InferredDifficulty.DI03_FLOW_DISORIENTATION
                ObservedInteractionSignal.OIS03_CONFIRMATION_PAUSE,
                ObservedInteractionSignal.OIS05_HELP_REQUEST,
                ObservedInteractionSignal.OIS08_SENSITIVE_ACTION -> InferredDifficulty.DI04_DOUBT_OR_NEED_FOR_CONTROL
                ObservedInteractionSignal.OIS07_ADAPTATION_REJECTED -> InferredDifficulty.DI05_USER_PREFERENCE_OR_LOSS_OF_CONTROL
            }
        }.distinct()
    }
}

class RulePlanner : AdaptationPlanner {
    override fun createPlan(
        difficulties: List<InferredDifficulty>,
        signals: List<ObservedInteractionSignal>,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot,
        event: AdaptiveInteractionEvent
    ): AdaptationPlan? {
        val rule = when {
            ObservedInteractionSignal.OIS03_CONFIRMATION_PAUSE in signals -> AdaptationRuleId.AR03
            ObservedInteractionSignal.OIS08_SENSITIVE_ACTION in signals -> AdaptationRuleId.AR08
            ObservedInteractionSignal.OIS06_FIELD_ERROR in signals -> AdaptationRuleId.AR06
            ObservedInteractionSignal.OIS05_HELP_REQUEST in signals -> AdaptationRuleId.AR05
            ObservedInteractionSignal.OIS04_BACKTRACKING in signals -> AdaptationRuleId.AR04
            ObservedInteractionSignal.OIS01_TOUCH_ERRORS in signals -> AdaptationRuleId.AR01
            ObservedInteractionSignal.OIS02_PROLONGED_TIME in signals -> AdaptationRuleId.AR02
            ObservedInteractionSignal.OIS07_ADAPTATION_REJECTED in signals -> AdaptationRuleId.AR07
            else -> return null
        }
        if (rule in knowledge.rejectedRulesForTask && rule != AdaptationRuleId.AR07) return null
        return rule.toPlan(signals, difficulties, taskState.taskId, taskState.screenId, event.reviewSummary)
    }
}

class SafetyAdaptationController : AdaptationController {
    override fun validatePlan(
        plan: AdaptationPlan,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): ControlledAdaptationPlan {
        if (plan.difficulties.isEmpty()) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "No difficulties identified.")

        val isSensitiveAction = plan.signals.contains(ObservedInteractionSignal.OIS08_SENSITIVE_ACTION)
        if (isSensitiveAction && plan.validationType == ValidationType.NON_INTRUSIVE) {
            return ControlledAdaptationPlan(plan.copy(validationType = ValidationType.EXPLICIT), true, ValidationResult.ADJUSTED, "Intrusive adaptation during sensitive action.")
        }

        val recentlyRejected = knowledge.lastRejectionAt?.let { System.currentTimeMillis() - it < 20_000 } == true
        if (recentlyRejected && plan.validationType == ValidationType.SUGGESTED) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "Recent rejection.")

        if (plan.validationType == ValidationType.SUGGESTED && knowledge.suggestionsShownForTask >= 2) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "Suggestion saturation.")
        if (plan.validationType == ValidationType.EXPLICIT && knowledge.modalShownForScreen) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "Modal already shown.")

        return ControlledAdaptationPlan(plan, true, ValidationResult.APPROVED)
    }
}

class ComposeAdaptationExecutor(private val knowledgeRepository: KnowledgeRepository) : AdaptationExecutor {
    override fun apply(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState {
        val previousStateSnapshot = mutableMapOf<String, Any>()
        plan.modifications.forEach { modification ->
            when (modification) {
                UiModification.UIM01_TEXT_SIZE -> previousStateSnapshot["textScale"] = currentState.textScale
                UiModification.UIM02_CONTRAST -> previousStateSnapshot["highContrast"] = currentState.highContrast
                UiModification.UIM03_TOUCH_TARGETS -> previousStateSnapshot["enlargedTouchTargets"] = currentState.enlargedTouchTargets
                UiModification.UIM04_SPACING -> previousStateSnapshot["increasedSpacing"] = currentState.increasedSpacing
                UiModification.UIM05_ICONS_LABELS -> previousStateSnapshot["showIconLabels"] = currentState.showIconLabels
                UiModification.UIM06_CONTEXTUAL_HELP -> {
                    previousStateSnapshot["contextualHelpVisible"] = currentState.contextualHelpVisible
                    previousStateSnapshot["contextualHelpMessage"] = currentState.contextualHelpMessage ?: ""
                }
                UiModification.UIM07_GUIDED_NAVIGATION -> {
                    previousStateSnapshot["guidedModeEnabled"] = currentState.guidedModeEnabled
                    previousStateSnapshot["guidedStepMessage"] = currentState.guidedStepMessage ?: ""
                }
                UiModification.UIM08_REINFORCED_CONFIRMATION -> previousStateSnapshot["reinforcedConfirmationVisible"] = currentState.reinforcedConfirmationVisible
                UiModification.UIM09_VISUAL_FEEDBACK -> {
                    previousStateSnapshot["contextualHelpVisible"] = currentState.contextualHelpVisible
                    previousStateSnapshot["contextualHelpMessage"] = currentState.contextualHelpMessage ?: ""
                }
                UiModification.UIM10_SAFE_EXIT -> previousStateSnapshot["safeExitEnabled"] = currentState.safeExitEnabled
            }
        }

        val state = plan.modifications.fold(currentState.copy(pendingAdaptation = null)) { acc, modification ->
            when (modification) {
                UiModification.UIM01_TEXT_SIZE -> acc.copy(textScale = 1.25f)
                UiModification.UIM02_CONTRAST -> acc.copy(highContrast = true)
                UiModification.UIM03_TOUCH_TARGETS -> acc.copy(enlargedTouchTargets = true)
                UiModification.UIM04_SPACING -> acc.copy(increasedSpacing = true)
                UiModification.UIM05_ICONS_LABELS -> acc.copy(showIconLabels = true)
                UiModification.UIM06_CONTEXTUAL_HELP -> acc.copy(contextualHelpVisible = true, contextualHelpMessage = plan.message)
                UiModification.UIM07_GUIDED_NAVIGATION -> acc.copy(guidedModeEnabled = true, guidedStepMessage = "Siga esta tarea paso a paso.")
                UiModification.UIM08_REINFORCED_CONFIRMATION -> acc.copy(reinforcedConfirmationVisible = true)
                UiModification.UIM09_VISUAL_FEEDBACK -> acc.copy(contextualHelpVisible = true, contextualHelpMessage = plan.message)
                UiModification.UIM10_SAFE_EXIT -> acc.copy(safeExitEnabled = true)
            }
        }

        if (plan.validationType == ValidationType.SUGGESTED) knowledgeRepository.rememberSuggested(plan.taskId, plan.ruleId)
        if (plan.validationType == ValidationType.EXPLICIT) knowledgeRepository.rememberModal(plan.screenId, plan.ruleId)

        val undoable = plan.ruleId in setOf(AdaptationRuleId.AR01, AdaptationRuleId.AR02, AdaptationRuleId.AR04)
        return state.copy(
            lastAppliedAdaptation = if (undoable) AppliedAdaptation(plan.ruleId, plan.modifications, previousStateSnapshot) else currentState.lastAppliedAdaptation,
            undoMessageVisible = undoable
        )
    }

    override fun undo(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState {
        knowledgeRepository.rememberRejected(plan.taskId, plan.ruleId)
        val lastApplied = currentState.lastAppliedAdaptation
        val restored = if (lastApplied != null && lastApplied.ruleId == plan.ruleId) {
            var tempState = currentState
            lastApplied.previousState.forEach { (key, value) ->
                tempState = when (key) {
                    "textScale" -> tempState.copy(textScale = value as Float)
                    "highContrast" -> tempState.copy(highContrast = value as Boolean)
                    "enlargedTouchTargets" -> tempState.copy(enlargedTouchTargets = value as Boolean)
                    "increasedSpacing" -> tempState.copy(increasedSpacing = value as Boolean)
                    "showIconLabels" -> tempState.copy(showIconLabels = value as Boolean)
                    "contextualHelpVisible" -> tempState.copy(contextualHelpVisible = value as Boolean)
                    "contextualHelpMessage" -> tempState.copy(contextualHelpMessage = (value as String).takeIf { it.isNotEmpty() })
                    "guidedModeEnabled" -> tempState.copy(guidedModeEnabled = value as Boolean)
                    "guidedStepMessage" -> tempState.copy(guidedStepMessage = (value as String).takeIf { it.isNotEmpty() })
                    "reinforcedConfirmationVisible" -> tempState.copy(reinforcedConfirmationVisible = value as Boolean)
                    "safeExitEnabled" -> tempState.copy(safeExitEnabled = value as Boolean)
                    else -> tempState
                }
            }
            tempState.copy(contextualHelpVisible = true, contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea.", undoMessageVisible = false, lastAppliedAdaptation = null)
        } else {
            currentState.copy(contextualHelpVisible = true, contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea.", undoMessageVisible = false, lastAppliedAdaptation = null)
        }
        return restored
    }
}

class ExtendedMapeKCoordinator(
    private val knowledgeRepository: KnowledgeRepository,
    private val monitor: InteractionMonitor = InteractionMonitorImpl(),
    private val analyzer: AdaptationAnalyzer = DifficultyAnalyzer(),
    private val planner: AdaptationPlanner = RulePlanner(),
    private val controller: AdaptationController = SafetyAdaptationController(),
    private val executor: AdaptationExecutor = ComposeAdaptationExecutor(knowledgeRepository)
) {
    fun clearEvents(taskId: TaskId? = null) = monitor.clearEvents(taskId)
    fun process(event: AdaptiveInteractionEvent, taskState: TaskInteractionState, currentState: AdaptiveUiState): AdaptationEngineResult {
        monitor.recordEvent(event)
        val signals = monitor.evaluateSignals(taskState)
        val knowledge = knowledgeRepository.snapshot(taskState.taskId, taskState.screenId)
        val difficulties = analyzer.inferDifficulties(signals, taskState, knowledge)
        val plan = planner.createPlan(difficulties, signals, taskState, knowledge, event) ?: return AdaptationEngineResult.NoAdaptation
        val controlled = controller.validatePlan(plan, taskState, knowledge)
        if (!controlled.canShow) return AdaptationEngineResult.NoAdaptation
        val activePlan = controlled.plan
        return when (activePlan.validationType) {
            ValidationType.NON_INTRUSIVE, ValidationType.DIRECT -> AdaptationEngineResult.Applied(executor.apply(activePlan, currentState), activePlan)
            ValidationType.SUGGESTED, ValidationType.EXPLICIT -> {
                val pending = PendingAdaptation(activePlan.ruleId, activePlan.title, activePlan.message, activePlan.modifications, activePlan.validationType, activePlan.reviewSummary)
                AdaptationEngineResult.RequiresUserValidation(currentState.copy(pendingAdaptation = pending), activePlan)
            }
            ValidationType.NOT_APPLICABLE -> AdaptationEngineResult.NoAdaptation
        }
    }
    fun apply(plan: AdaptationPlan, currentState: AdaptiveUiState) = executor.apply(plan, currentState)
    fun undo(plan: AdaptationPlan, currentState: AdaptiveUiState) = executor.undo(plan, currentState)
    fun reject(plan: AdaptationPlan) = knowledgeRepository.rememberRejected(plan.taskId, plan.ruleId)
}

private fun AdaptationRuleId.toPlan(signals: List<ObservedInteractionSignal>, difficulties: List<InferredDifficulty>, taskId: TaskId?, screenId: ScreenId?, reviewSummary: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary? = null): AdaptationPlan {
    val id = "evt_" + UUID.randomUUID().toString()
    return when (this) {
        AdaptationRuleId.AR01 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM03_TOUCH_TARGETS, UiModification.UIM04_SPACING), ValidationType.SUGGESTED, "Facilitar los toques", "Puedo hacer los botones más grandes y separados para reducir toques accidentales.", taskId, screenId)
        AdaptationRuleId.AR02 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM01_TEXT_SIZE, UiModification.UIM02_CONTRAST, UiModification.UIM06_CONTEXTUAL_HELP, UiModification.UIM09_VISUAL_FEEDBACK), ValidationType.NON_INTRUSIVE, "Siguiente paso sugerido", "Aumenté el texto y el contraste. Siguiente paso: revise la información principal y toque el botón para continuar.", taskId, screenId)
        AdaptationRuleId.AR03 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM08_REINFORCED_CONFIRMATION, UiModification.UIM10_SAFE_EXIT, UiModification.UIM06_CONTEXTUAL_HELP), ValidationType.EXPLICIT, "Revisar antes de guardar", "Antes de guardar, revise el resumen.", taskId, screenId, reviewSummary)
        AdaptationRuleId.AR04 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM05_ICONS_LABELS, UiModification.UIM07_GUIDED_NAVIGATION, UiModification.UIM09_VISUAL_FEEDBACK), ValidationType.SUGGESTED, "Activar guía paso a paso", "Puedo mostrar etiquetas y una guía para completar esta tarea paso a paso.", taskId, screenId)
        AdaptationRuleId.AR05 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM05_ICONS_LABELS, UiModification.UIM06_CONTEXTUAL_HELP), ValidationType.DIRECT, "Ayuda disponible", "Esta pantalla usa datos ficticios. Revise el dato solicitado, complete el control principal y use Volver o Cancelar si necesita corregir.", taskId, screenId)
        AdaptationRuleId.AR06 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM09_VISUAL_FEEDBACK, UiModification.UIM10_SAFE_EXIT), ValidationType.DIRECT, "Revise el campo", "Ingrese un número entre 1 y 10.", taskId, screenId)
        AdaptationRuleId.AR07 -> AdaptationPlan(id, this, signals, difficulties, emptyList(), ValidationType.NOT_APPLICABLE, "Preferencia guardada", "Entendido.", taskId, screenId)
        AdaptationRuleId.AR08 -> AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM08_REINFORCED_CONFIRMATION, UiModification.UIM10_SAFE_EXIT), ValidationType.EXPLICIT, "Revisar antes de continuar", "Está por guardar información simulada.", taskId, screenId, reviewSummary)
    }
}
