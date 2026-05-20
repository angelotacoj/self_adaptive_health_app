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
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.KnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.logging.MapeKLog

sealed interface AdaptationEngineResult {
    data object NoAdaptation : AdaptationEngineResult
    data class Applied(val state: AdaptiveUiState, val plan: AdaptationPlan) : AdaptationEngineResult
    data class RequiresUserValidation(val state: AdaptiveUiState, val plan: AdaptationPlan) : AdaptationEngineResult
}

interface InteractionMonitor {
    fun recordEvent(event: AdaptiveInteractionEvent)
    fun evaluateSignals(taskState: TaskInteractionState): List<ObservedInteractionSignal>
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
        knowledge: KnowledgeSnapshot
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

class ExtendedMapeKEngine(
    private val knowledgeRepository: KnowledgeRepository
) : InteractionMonitor, AdaptationAnalyzer, AdaptationPlanner, AdaptationController, AdaptationExecutor {
    private val events = mutableListOf<AdaptiveInteractionEvent>()

    fun clearEvents(taskId: TaskId? = null) {
        if (taskId == null) {
            events.clear()
            MapeKLog.stage("MONITOR", "event buffer cleared scope=ALL")
        } else {
            events.removeAll { it.taskId == taskId }
            MapeKLog.stage("MONITOR", "event buffer cleared task=$taskId")
        }
    }

    fun process(
        event: AdaptiveInteractionEvent,
        taskState: TaskInteractionState,
        currentState: AdaptiveUiState
    ): AdaptationEngineResult {
        recordEvent(event)
        val signals = evaluateSignals(taskState)
        val knowledge = knowledgeRepository.snapshot(taskState.taskId, taskState.screenId)
        val difficulties = inferDifficulties(signals, taskState, knowledge)
        val plan = createPlan(difficulties, signals, taskState, knowledge) ?: run {
            MapeKLog.stage(
                "STATE",
                "trace=${event.eventType} -> OIS=${signals.names()} -> DI=${difficulties.names()} -> AR=NONE -> validation=NOT_APPLICABLE -> UI=NONE -> knowledge=PENDING_NAV_SAVE"
            )
            return AdaptationEngineResult.NoAdaptation
        }
        val controlled = validatePlan(plan, taskState, knowledge)
        if (!controlled.canShow) {
            MapeKLog.stage(
                "STATE",
                "trace=${event.eventType} -> OIS=${signals.names()} -> DI=${difficulties.names()} -> AR=${plan.ruleId} -> validation=BLOCKED(${controlled.reason}) -> UI=NONE -> knowledge=PENDING_NAV_SAVE"
            )
            return AdaptationEngineResult.NoAdaptation
        }

        return when (plan.validationType) {
            ValidationType.NON_INTRUSIVE,
            ValidationType.DIRECT -> {
                MapeKLog.stage(
                    "STATE",
                    "trace=${event.eventType} -> OIS=${signals.names()} -> DI=${difficulties.names()} -> AR=${plan.ruleId} -> validation=${plan.validationType}_AUTO_APPROVED -> UI=${plan.modifications.names()} -> knowledge=PENDING_NAV_SAVE"
                )
                AdaptationEngineResult.Applied(apply(plan, currentState), plan)
            }

            ValidationType.SUGGESTED,
            ValidationType.EXPLICIT -> {
                val pending = PendingAdaptation(plan.ruleId, plan.title, plan.message, plan.modifications, plan.validationType)
                MapeKLog.stage(
                    "USER_VALIDATION",
                    "trace=${event.eventType} -> OIS=${signals.names()} -> DI=${difficulties.names()} -> AR=${plan.ruleId} -> validation=${plan.validationType}_PENDING -> UI=PENDING_USER_DECISION -> knowledge=PENDING_NAV_SAVE"
                )
                AdaptationEngineResult.RequiresUserValidation(currentState.copy(pendingAdaptation = pending), plan)
            }

            ValidationType.NOT_APPLICABLE -> AdaptationEngineResult.NoAdaptation
        }
    }

    override fun recordEvent(event: AdaptiveInteractionEvent) {
        events += event
        MapeKLog.stage("MONITOR", "input event=${event.eventType} task=${event.taskId} screen=${event.screenId}")
        MapeKLog.stage("MONITOR", "interaction event=${event.eventType} task=${event.taskId} screen=${event.screenId}")
    }

    override fun evaluateSignals(taskState: TaskInteractionState): List<ObservedInteractionSignal> {
        val taskEvents = events.filter { it.taskId == taskState.taskId }
        val screenEvents = taskEvents.filter { it.screenId == taskState.screenId }
        val signals = mutableListOf<ObservedInteractionSignal>()
        if (screenEvents.count { it.eventType == AdaptiveInteractionEventType.TOUCH_ERROR } >= 3 ||
            taskEvents.count { it.eventType == AdaptiveInteractionEventType.TOUCH_ERROR } >= 4
        ) signals += ObservedInteractionSignal.OIS01_TOUCH_ERRORS
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.PROLONGED_TIME }) signals += ObservedInteractionSignal.OIS02_PROLONGED_TIME
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.CONFIRMATION_PAUSE }) signals += ObservedInteractionSignal.OIS03_CONFIRMATION_PAUSE
        if (taskEvents.count { it.eventType == AdaptiveInteractionEventType.BACK_PRESSED } >= 2) signals += ObservedInteractionSignal.OIS04_BACKTRACKING
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.HELP_REQUESTED }) signals += ObservedInteractionSignal.OIS05_HELP_REQUEST
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.FIELD_ERROR } || taskState.fieldErrorCount > 0) signals += ObservedInteractionSignal.OIS06_FIELD_ERROR
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.ADAPTATION_REJECTED }) signals += ObservedInteractionSignal.OIS07_ADAPTATION_REJECTED
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.SENSITIVE_ACTION }) signals += ObservedInteractionSignal.OIS08_SENSITIVE_ACTION
        MapeKLog.stage("MONITOR", "output OIS=${signals.names()} task=${taskState.taskId} screen=${taskState.screenId}")
        return signals
    }

    override fun inferDifficulties(
        signals: List<ObservedInteractionSignal>,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): List<InferredDifficulty> {
        val difficulties = signals.mapNotNull {
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
        MapeKLog.stage("ANALYZER", "input OIS=${signals.names()} task=${taskState.taskId} screen=${taskState.screenId}")
        MapeKLog.stage("ANALYZER", "output DI=${difficulties.names()} rejected=${knowledge.rejectedRulesForTask.names()}")
        return difficulties
    }

    override fun createPlan(
        difficulties: List<InferredDifficulty>,
        signals: List<ObservedInteractionSignal>,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
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
            else -> {
                MapeKLog.stage("PLANNER", "AR=NONE because OIS=${signals.names()}")
                return null
            }
        }
        if (rule in knowledge.rejectedRulesForTask && rule != AdaptationRuleId.AR07) {
            MapeKLog.stage("PLANNER", "AR=$rule suppressed by Knowledge Base rejection memory")
            return null
        }
        val plan = rule.toPlan(signals, difficulties, taskState.taskId, taskState.screenId)
        MapeKLog.stage("PLANNER", "output AR=${plan.ruleId} UIM=${plan.modifications.names()} validation=${plan.validationType}")
        return plan
    }

    override fun validatePlan(
        plan: AdaptationPlan,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): ControlledAdaptationPlan {
        val recentlyRejected = knowledge.lastRejectionAt?.let { System.currentTimeMillis() - it < 20_000 } == true
        if (recentlyRejected && plan.validationType == ValidationType.SUGGESTED) {
            MapeKLog.stage("CONTROLLER", "decision=BLOCKED reason=Recent rejection. AR=${plan.ruleId}")
            MapeKLog.stage("CONTROLLER", "validation decision=BLOCKED_RECENT_REJECTION AR=${plan.ruleId}")
            return ControlledAdaptationPlan(plan, false, "Recent rejection.")
        }
        if (plan.validationType == ValidationType.SUGGESTED && knowledge.suggestionsShownForTask >= 2) {
            MapeKLog.stage("CONTROLLER", "decision=BLOCKED reason=Suggestion saturation. AR=${plan.ruleId}")
            MapeKLog.stage("CONTROLLER", "validation decision=BLOCKED_SUGGESTION_SATURATION AR=${plan.ruleId}")
            return ControlledAdaptationPlan(plan, false, "Suggestion saturation.")
        }
        if (plan.validationType == ValidationType.EXPLICIT && knowledge.modalShownForScreen) {
            MapeKLog.stage("CONTROLLER", "decision=BLOCKED reason=Modal already shown on screen. AR=${plan.ruleId}")
            MapeKLog.stage("CONTROLLER", "validation decision=BLOCKED_MODAL_ALREADY_SHOWN AR=${plan.ruleId}")
            return ControlledAdaptationPlan(plan, false, "Modal already shown on screen.")
        }
        MapeKLog.stage("CONTROLLER", "decision=CAN_SHOW reason=Allowed AR=${plan.ruleId}")
        MapeKLog.stage("CONTROLLER", "validation decision=CAN_SHOW AR=${plan.ruleId} type=${plan.validationType}")
        return ControlledAdaptationPlan(plan, true)
    }

    override fun apply(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState {
        MapeKLog.stage("EXECUTOR", "applying AR=${plan.ruleId} UI=${plan.modifications.names()}")
        MapeKLog.stage("EXECUTOR", "before adaptiveUiState=$currentState")
        MapeKLog.stage(
            "EXECUTOR",
            "before textScale=${currentState.textScale} contextualHelpVisible=${currentState.contextualHelpVisible}"
        )
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
        MapeKLog.stage("EXECUTOR", "after adaptiveUiState=$state")
        MapeKLog.stage(
            "EXECUTOR",
            "after textScale=${state.textScale} contextualHelpVisible=${state.contextualHelpVisible}"
        )
        if (plan.validationType == ValidationType.SUGGESTED) {
            knowledgeRepository.rememberSuggested(plan.taskId, plan.ruleId)
            MapeKLog.stage("KNOWLEDGE", "rememberSuggested task=${plan.taskId} AR=${plan.ruleId}")
        }
        if (plan.validationType == ValidationType.EXPLICIT) {
            knowledgeRepository.rememberModal(plan.screenId, plan.ruleId)
            MapeKLog.stage("KNOWLEDGE", "rememberModal screen=${plan.screenId} AR=${plan.ruleId}")
        }
        MapeKLog.stage("STATE", "AdaptiveUiState changed by AR=${plan.ruleId}; Compose observes StateFlow and recomposes")
        MapeKLog.state("adaptiveUiState updated textScale=${state.textScale} contextualHelpVisible=${state.contextualHelpVisible}")
        val undoable = plan.ruleId in setOf(AdaptationRuleId.AR01, AdaptationRuleId.AR02, AdaptationRuleId.AR04)
        return state.copy(
            lastAppliedAdaptation = if (undoable) AppliedAdaptation(plan.ruleId, plan.modifications) else currentState.lastAppliedAdaptation,
            undoMessageVisible = undoable
        )
    }

    override fun undo(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState {
        knowledgeRepository.rememberRejected(plan.taskId, plan.ruleId)
        MapeKLog.stage("USER_VALIDATION", "validation decision=UNDO AR=${plan.ruleId}")
        MapeKLog.stage("KNOWLEDGE", "rememberRejected from undo task=${plan.taskId} AR=${plan.ruleId}")
        val restored = currentState.copy(
            textScale = if (UiModification.UIM01_TEXT_SIZE in plan.modifications) 1.0f else currentState.textScale,
            enlargedTouchTargets = if (UiModification.UIM03_TOUCH_TARGETS in plan.modifications) false else currentState.enlargedTouchTargets,
            increasedSpacing = if (UiModification.UIM04_SPACING in plan.modifications) false else currentState.increasedSpacing,
            guidedModeEnabled = if (UiModification.UIM07_GUIDED_NAVIGATION in plan.modifications) false else currentState.guidedModeEnabled,
            guidedStepMessage = if (UiModification.UIM07_GUIDED_NAVIGATION in plan.modifications) null else currentState.guidedStepMessage,
            contextualHelpVisible = true,
            contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea.",
            undoMessageVisible = false,
            lastAppliedAdaptation = null
        )
        MapeKLog.stage(
            "EXECUTOR",
            "undo AR=${plan.ruleId} textScale ${currentState.textScale} -> ${restored.textScale}"
        )
        MapeKLog.state("adaptiveUiState updated textScale=${restored.textScale} contextualHelpVisible=${restored.contextualHelpVisible}")
        return restored
    }

    fun reject(plan: AdaptationPlan) {
        knowledgeRepository.rememberRejected(plan.taskId, plan.ruleId)
        MapeKLog.stage("USER_VALIDATION", "validation decision=REJECT AR=${plan.ruleId}")
        MapeKLog.stage("KNOWLEDGE", "rememberRejected task=${plan.taskId} AR=${plan.ruleId}")
    }
}

private fun <T : Enum<T>> Collection<T>.names(): String = joinToString(prefix = "[", postfix = "]", separator = ",") { it.name }

private fun AdaptationRuleId.toPlan(
    signals: List<ObservedInteractionSignal>,
    difficulties: List<InferredDifficulty>,
    taskId: TaskId?,
    screenId: ScreenId?
): AdaptationPlan {
    return when (this) {
        AdaptationRuleId.AR01 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM03_TOUCH_TARGETS, UiModification.UIM04_SPACING), ValidationType.SUGGESTED, "Facilitar los toques", "Puedo hacer los botones más grandes y separados para facilitar los toques. ¿Desea aplicar este cambio?", taskId, screenId)
        AdaptationRuleId.AR02 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM01_TEXT_SIZE, UiModification.UIM06_CONTEXTUAL_HELP, UiModification.UIM09_VISUAL_FEEDBACK), ValidationType.NON_INTRUSIVE, "Siguiente paso sugerido", "Siguiente paso sugerido: revise la información y toque el botón para continuar.", taskId, screenId)
        AdaptationRuleId.AR03 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM08_REINFORCED_CONFIRMATION, UiModification.UIM10_SAFE_EXIT), ValidationType.EXPLICIT, "Revisar antes de guardar", "Antes de guardar, revise el resumen. Puede confirmar, editar o cancelar.", taskId, screenId)
        AdaptationRuleId.AR04 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM07_GUIDED_NAVIGATION, UiModification.UIM09_VISUAL_FEEDBACK), ValidationType.SUGGESTED, "Activar guía paso a paso", "Puedo mostrar esta tarea paso a paso. ¿Desea activar la guía?", taskId, screenId)
        AdaptationRuleId.AR05 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM06_CONTEXTUAL_HELP), ValidationType.DIRECT, "Ayuda disponible", "Esta pantalla usa datos ficticios. Lea la tarjeta y luego elija la acción más clara para continuar.", taskId, screenId)
        AdaptationRuleId.AR06 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM09_VISUAL_FEEDBACK, UiModification.UIM10_SAFE_EXIT), ValidationType.DIRECT, "Revise el campo", "Ingrese un número entre 1 y 10. Ejemplo: 7.", taskId, screenId)
        AdaptationRuleId.AR07 -> AdaptationPlan(this, signals, difficulties, emptyList(), ValidationType.NOT_APPLICABLE, "Preferencia guardada", "Entendido. No volveré a mostrar esta sugerencia durante esta tarea.", taskId, screenId)
        AdaptationRuleId.AR08 -> AdaptationPlan(this, signals, difficulties, listOf(UiModification.UIM08_REINFORCED_CONFIRMATION, UiModification.UIM10_SAFE_EXIT), ValidationType.EXPLICIT, "Revisar antes de continuar", "Está por guardar información simulada. Revise los datos antes de continuar.", taskId, screenId)
    }
}
