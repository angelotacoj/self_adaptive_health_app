package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
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
    data class Suppressed(val ruleId: AdaptationRuleId, val reason: String, val level: AdaptationLevel? = null) : AdaptationEngineResult
    data class Applied(val state: AdaptiveUiState, val plan: AdaptationPlan) : AdaptationEngineResult
    data class RequiresUserValidation(val state: AdaptiveUiState, val plan: AdaptationPlan) : AdaptationEngineResult
}

class MaxLevelReachedException(val ruleId: AdaptationRuleId) : Exception("Max level reached")

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
        event: AdaptiveInteractionEvent,
        currentState: AdaptiveUiState
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

        if (screenEvents.count { it.eventType == AdaptiveInteractionEventType.TOUCH_ERROR } >= 2) {
            signals += ObservedInteractionSignal.OIS04_FIELD_ERROR
        }

        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.PROLONGED_TIME }) signals += ObservedInteractionSignal.OIS01_TIME_ON_SCREEN
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.CONFIRMATION_PAUSE }) signals += ObservedInteractionSignal.OIS05_CONFIRMATION_PAUSE
        if (taskEvents.count { it.eventType == AdaptiveInteractionEventType.BACK_PRESSED } >= 2) signals += ObservedInteractionSignal.OIS02_BACKTRACKING
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.HELP_REQUESTED }) signals += ObservedInteractionSignal.OIS03_HELP_REQUEST
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.FIELD_ERROR } || taskState.fieldErrorCount > 0) signals += ObservedInteractionSignal.OIS04_FIELD_ERROR
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.ADAPTATION_REJECTED }) signals += ObservedInteractionSignal.OIS03_HELP_REQUEST
        if (taskEvents.any { it.eventType == AdaptiveInteractionEventType.SENSITIVE_ACTION }) signals += ObservedInteractionSignal.OIS05_CONFIRMATION_PAUSE

        return signals.distinct()
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
                ObservedInteractionSignal.OIS01_TIME_ON_SCREEN -> InferredDifficulty.DI01_LEGIBILITY_DIFFICULTY
                ObservedInteractionSignal.OIS02_BACKTRACKING -> InferredDifficulty.DI02_COMPREHENSION_ORIENTATION_DIFFICULTY
                ObservedInteractionSignal.OIS03_HELP_REQUEST -> InferredDifficulty.DI02_COMPREHENSION_ORIENTATION_DIFFICULTY
                ObservedInteractionSignal.OIS04_FIELD_ERROR -> InferredDifficulty.DI03_RECOVERY_UNCERTAINTY_DIFFICULTY
                ObservedInteractionSignal.OIS05_CONFIRMATION_PAUSE -> InferredDifficulty.DI04_CONTROL_CONFIRMATION_DOUBT
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
        event: AdaptiveInteractionEvent,
        currentState: AdaptiveUiState
    ): AdaptationPlan? {
        val rule = when {
            ObservedInteractionSignal.OIS05_CONFIRMATION_PAUSE in signals -> AdaptationRuleId.AR05_CONFIRMATION_PAUSE
            ObservedInteractionSignal.OIS04_FIELD_ERROR in signals -> AdaptationRuleId.AR04_FIELD_ERROR
            ObservedInteractionSignal.OIS03_HELP_REQUEST in signals -> AdaptationRuleId.AR03_HELP_REQUEST
            ObservedInteractionSignal.OIS02_BACKTRACKING in signals -> AdaptationRuleId.AR02_BACKTRACKING
            ObservedInteractionSignal.OIS01_TIME_ON_SCREEN in signals -> AdaptationRuleId.AR01_TIME_ON_SCREEN
            else -> return null
        }

        // Determine current support level of the target component
        val currentLevel = when (rule) {
            AdaptationRuleId.AR01_TIME_ON_SCREEN -> currentState.uim01Level
            AdaptationRuleId.AR02_BACKTRACKING -> currentState.uim02Level
            AdaptationRuleId.AR03_HELP_REQUEST -> currentState.uim02Level
            AdaptationRuleId.AR04_FIELD_ERROR -> currentState.uim04Level
            AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> currentState.uim03Level
        }

        // Calculate next level using the skip-rejected loop policy
        val targetLevel = getNextValidLevel(rule, currentLevel, knowledge) ?: return null

        return rule.toPlan(signals, difficulties, taskState.taskId, taskState.screenId, targetLevel, event.reviewSummary)
    }

    private fun getNextValidLevel(
        ruleId: AdaptationRuleId,
        currentLevel: AdaptationLevel,
        knowledge: KnowledgeSnapshot
    ): AdaptationLevel? {
        var nextLevelVal = currentLevel.levelValue + 1
        while (nextLevelVal <= AdaptationLevel.LEVEL_3_HIGH_SUPPORT.levelValue) {
            val candidate = AdaptationLevel.entries.find { it.levelValue == nextLevelVal } ?: break
            val isRejected = Pair(ruleId, candidate) in knowledge.rejectedRuleLevelsForTask
            if (!isRejected) {
                return candidate
            }
            nextLevelVal++
        }
        if (currentLevel == AdaptationLevel.LEVEL_3_HIGH_SUPPORT || nextLevelVal > AdaptationLevel.LEVEL_3_HIGH_SUPPORT.levelValue) {
            throw MaxLevelReachedException(ruleId)
        }
        return null
    }
}

class SafetyAdaptationController : AdaptationController {
    override fun validatePlan(
        plan: AdaptationPlan,
        taskState: TaskInteractionState,
        knowledge: KnowledgeSnapshot
    ): ControlledAdaptationPlan {
        if (plan.difficulties.isEmpty()) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "No difficulties identified.")

        val isSensitiveAction = plan.signals.contains(ObservedInteractionSignal.OIS05_CONFIRMATION_PAUSE)
        if (isSensitiveAction && plan.validationType == ValidationType.NON_INTRUSIVE) {
            return ControlledAdaptationPlan(plan.copy(validationType = ValidationType.EXPLICIT), true, ValidationResult.ADJUSTED, "Intrusive adaptation during sensitive action.")
        }

        val recentlyRejected = knowledge.lastRejectionAt?.let { System.currentTimeMillis() - it < 60_000 } == true
        if (recentlyRejected && plan.validationType == ValidationType.SUGGESTED) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "RECENT_REJECTION_COOLDOWN")

        if (plan.validationType == ValidationType.SUGGESTED && knowledge.suggestionsShownForTask >= 3) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "SUGGESTION_SATURATION")
        if (plan.validationType == ValidationType.EXPLICIT && knowledge.modalShownForScreen) return ControlledAdaptationPlan(plan, false, ValidationResult.REJECTED, "Modal already shown.")

        return ControlledAdaptationPlan(plan, true, ValidationResult.APPROVED)
    }
}

class ComposeAdaptationExecutor(private val knowledgeRepository: KnowledgeRepository) : AdaptationExecutor {
    override fun apply(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState {
        val previousStateSnapshot = mapOf<String, Any>(
            "uim01Level" to currentState.uim01Level,
            "uim02Level" to currentState.uim02Level,
            "uim03Level" to currentState.uim03Level,
            "uim04Level" to currentState.uim04Level
        )

        var state = currentState.copy(pendingAdaptation = null)
        plan.modifications.forEach { modification ->
            state = when (modification) {
                UiModification.UIM01_TEXT -> state.copy(uim01Level = plan.targetLevel)
                UiModification.UIM02_CONTEXTUAL_HELP_STEP_BY_STEP -> {
                    state.copy(
                        uim02Level = plan.targetLevel,
                        contextualHelpMessage = plan.message,
                        guidedStepMessage = if (plan.targetLevel == AdaptationLevel.LEVEL_3_HIGH_SUPPORT) plan.message else null
                    )
                }
                UiModification.UIM03_REINFORCED_CONFIRMATION -> state.copy(uim03Level = plan.targetLevel)
                UiModification.UIM04_VISUAL_FEEDBACK -> {
                    state.copy(
                        uim04Level = plan.targetLevel,
                        contextualHelpMessage = plan.message
                    )
                }
            }
        }

        if (plan.validationType == ValidationType.SUGGESTED) knowledgeRepository.rememberSuggested(plan.taskId, plan.ruleId)
        if (plan.validationType == ValidationType.EXPLICIT) knowledgeRepository.rememberModal(plan.screenId, plan.ruleId)

        val undoable = plan.ruleId in setOf(AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationRuleId.AR02_BACKTRACKING)
        return state.copy(
            lastAppliedAdaptation = if (undoable) AppliedAdaptation(plan.ruleId, plan.modifications, previousStateSnapshot, level = plan.targetLevel) else currentState.lastAppliedAdaptation,
            undoMessageVisible = undoable
        )
    }

    override fun undo(plan: AdaptationPlan, currentState: AdaptiveUiState): AdaptiveUiState {
        knowledgeRepository.rememberRejected(plan.taskId, plan.ruleId, plan.targetLevel)
        val lastApplied = currentState.lastAppliedAdaptation
        val restored = if (lastApplied != null && lastApplied.ruleId == plan.ruleId) {
            val uim01 = lastApplied.previousState["uim01Level"] as? AdaptationLevel ?: AdaptationLevel.LEVEL_0_BASE
            val uim02 = lastApplied.previousState["uim02Level"] as? AdaptationLevel ?: AdaptationLevel.LEVEL_0_BASE
            val uim03 = lastApplied.previousState["uim03Level"] as? AdaptationLevel ?: AdaptationLevel.LEVEL_0_BASE
            val uim04 = lastApplied.previousState["uim04Level"] as? AdaptationLevel ?: AdaptationLevel.LEVEL_0_BASE
            currentState.copy(
                uim01Level = uim01,
                uim02Level = uim02,
                uim03Level = uim03,
                uim04Level = uim04,
                contextualHelpVisibleOverride = true,
                contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea.",
                undoMessageVisible = false,
                lastAppliedAdaptation = null
            )
        } else {
            currentState.copy(
                contextualHelpVisibleOverride = true,
                contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea.",
                undoMessageVisible = false,
                lastAppliedAdaptation = null
            )
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
        
        // Log evaluated signals using Log.i
        android.util.Log.i("AURA_MAPEK", "MONITOR | Signals evaluated: $signals for task=${taskState.taskId} screen=${taskState.screenId}")
        
        val knowledge = knowledgeRepository.snapshot(taskState.taskId, taskState.screenId)
        val difficulties = analyzer.inferDifficulties(signals, taskState, knowledge)
        
        // Log inferred difficulties using Log.i
        if (difficulties.isNotEmpty()) {
            android.util.Log.i("AURA_MAPEK", "ANALYZER | Inferred difficulties: $difficulties")
        }

        val plan = try {
            planner.createPlan(difficulties, signals, taskState, knowledge, event, currentState)
        } catch (e: MaxLevelReachedException) {
            return AdaptationEngineResult.Suppressed(e.ruleId, "MAX_LEVEL_REACHED", AdaptationLevel.LEVEL_3_HIGH_SUPPORT)
        }
        if (plan == null) {
            return AdaptationEngineResult.NoAdaptation
        }
        
        // Log proposed plan using Log.i
        android.util.Log.i("AURA_MAPEK", "PLANNER | Proposed plan: rule=${plan.ruleId} targetLevel=${plan.targetLevel} modifications=${plan.modifications}")

        val controlled = controller.validatePlan(plan, taskState, knowledge)
        
        // Log safety validation result using Log.i
        android.util.Log.i("AURA_MAPEK", "CONTROLLER | Validation result: ${controlled.result} canShow=${controlled.canShow} reason=${controlled.reason ?: "Approved"}")

        if (!controlled.canShow) return AdaptationEngineResult.Suppressed(plan.ruleId, controlled.reason ?: "SUPPRESSED", plan.targetLevel)
        val activePlan = controlled.plan
        return when (activePlan.validationType) {
            ValidationType.NON_INTRUSIVE, ValidationType.DIRECT -> {
                android.util.Log.i("AURA_MAPEK", "EXECUTOR | Applying plan directly (DIRECT/NON_INTRUSIVE)")
                AdaptationEngineResult.Applied(executor.apply(activePlan, currentState), activePlan)
            }
            ValidationType.SUGGESTED, ValidationType.EXPLICIT -> {
                android.util.Log.i("AURA_MAPEK", "EXECUTOR | Requesting user validation (SUGGESTED/EXPLICIT)")
                val pending = PendingAdaptation(activePlan.ruleId, activePlan.title, activePlan.message, activePlan.modifications, activePlan.validationType, activePlan.reviewSummary)
                AdaptationEngineResult.RequiresUserValidation(currentState.copy(pendingAdaptation = pending), activePlan)
            }
            ValidationType.NOT_APPLICABLE -> AdaptationEngineResult.NoAdaptation
        }
    }
    fun apply(plan: AdaptationPlan, currentState: AdaptiveUiState) = executor.apply(plan, currentState)
    fun undo(plan: AdaptationPlan, currentState: AdaptiveUiState) = executor.undo(plan, currentState)
    fun reject(plan: AdaptationPlan) = knowledgeRepository.rememberRejected(plan.taskId, plan.ruleId, plan.targetLevel)
}

private fun AdaptationRuleId.toPlan(
    signals: List<ObservedInteractionSignal>,
    difficulties: List<InferredDifficulty>,
    taskId: TaskId?,
    screenId: ScreenId?,
    targetLevel: AdaptationLevel,
    reviewSummary: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary? = null
): AdaptationPlan {
    val id = "evt_" + UUID.randomUUID().toString()
    return when (this) {
        AdaptationRuleId.AR01_TIME_ON_SCREEN -> {
            val (title, msg) = when (targetLevel) {
                AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> Pair("Aumentar texto", "¿Desea aumentar ligeramente el tamaño del texto para leer mejor?")
                AdaptationLevel.LEVEL_2_MODERATE_SUPPORT -> Pair("Ajustar contraste y texto", "¿Desea aplicar letras más grandes y contraste alto para facilitar la lectura?")
                else -> Pair("Simplificar botones y espaciado", "¿Desea usar botones más grandes, mayor separación y texto máximo?")
            }
            AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM01_TEXT, UiModification.UIM02_CONTEXTUAL_HELP_STEP_BY_STEP), ValidationType.NON_INTRUSIVE, title, msg, taskId, screenId, reviewSummary, targetLevel)
        }
        AdaptationRuleId.AR02_BACKTRACKING -> {
            val (title, msg) = when (targetLevel) {
                AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> Pair("Mostrar etiquetas de navegación", "¿Desea mostrar etiquetas de texto debajo de los íconos de navegación?")
                AdaptationLevel.LEVEL_2_MODERATE_SUPPORT -> Pair("Activar ayuda en pantalla", "¿Desea ver un panel de ayuda con explicaciones sobre esta pantalla?")
                else -> Pair("Activar guía paso a paso", "¿Desea que le guíe paso a paso en esta tarea?")
            }
            AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM02_CONTEXTUAL_HELP_STEP_BY_STEP), ValidationType.SUGGESTED, title, msg, taskId, screenId, reviewSummary, targetLevel)
        }
        AdaptationRuleId.AR03_HELP_REQUEST -> {
            val msg = when (targetLevel) {
                AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> "He activado etiquetas descriptivas debajo de los íconos."
                AdaptationLevel.LEVEL_2_MODERATE_SUPPORT -> "He abierto el panel de ayuda contextual para esta tarea."
                else -> "He activado el asistente paso a paso para guiarle."
            }
            AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM02_CONTEXTUAL_HELP_STEP_BY_STEP), ValidationType.DIRECT, "Ayuda disponible", msg, taskId, screenId, reviewSummary, targetLevel)
        }
        AdaptationRuleId.AR04_FIELD_ERROR -> {
            val msg = when (targetLevel) {
                AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> "Revise el campo resaltado; asegúrese de ingresar el formato correcto."
                AdaptationLevel.LEVEL_2_MODERATE_SUPPORT -> "Ejemplo: Ingrese un número o el texto según el formato solicitado en pantalla."
                else -> "He resaltado el campo y activado la ayuda persistente para corregir el dato."
            }
            AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM04_VISUAL_FEEDBACK), ValidationType.DIRECT, "Revise el campo", msg, taskId, screenId, reviewSummary, targetLevel)
        }
        AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> {
            val title = if (taskId != TaskId.T5_SUMMARY || reviewSummary != null) "Revisar antes de continuar" else "Revisar antes de guardar"
            val msg = when (targetLevel) {
                AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> "Antes de continuar, asegúrese de revisar la información ingresada."
                AdaptationLevel.LEVEL_2_MODERATE_SUPPORT -> "Por favor, revise este resumen de sus datos antes de guardar."
                else -> "Por favor, confirme cada uno de los siguientes datos ficticios para finalizar la tarea."
            }
            AdaptationPlan(id, this, signals, difficulties, listOf(UiModification.UIM03_REINFORCED_CONFIRMATION), ValidationType.EXPLICIT, title, msg, taskId, screenId, reviewSummary, targetLevel)
        }
    }
}
