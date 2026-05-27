package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptationEngineResult
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.ExtendedMapeKCoordinator
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationPlan
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEvent
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.TaskInteractionState
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.KnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.logging.InteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskStateEntity
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AdaptiveViewModel(
    private val coordinator: ExtendedMapeKCoordinator,
    private val knowledge: KnowledgeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdaptiveUiState())
    val uiState: StateFlow<AdaptiveUiState> = _uiState.asStateFlow()

    private var pendingPlan: AdaptationPlan? = null
    private var lastAppliedPlan: AdaptationPlan? = null
    private val persistentPreferences = mutableSetOf<UiModification>()
    
    // Map to keep track of real interaction state per task/screen
    private val taskInteractionStates = mutableMapOf<Pair<TaskId?, ScreenId?>, TaskInteractionState>()

    fun setAdaptiveMode(isAdaptive: Boolean) {
        _uiState.update { it.copy(isAdaptiveMode = isAdaptive) }
    }

    fun updateStateFromProfile(newState: AdaptiveUiState) {
        _uiState.value = newState
    }
    
    fun resetState() {
        _uiState.value = AdaptiveUiState()
        pendingPlan = null
        lastAppliedPlan = null
        persistentPreferences.clear()
        taskInteractionStates.clear()
    }

    fun resetTemporaryStateForTask(isAdaptive: Boolean) {
        pendingPlan = null
        lastAppliedPlan = null
        _uiState.value = baselineState(isAdaptive)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearEvents(taskId: TaskId? = null) {
        coordinator.clearEvents(taskId)
    }

    private fun baselineState(isAdaptive: Boolean): AdaptiveUiState {
        if (!isAdaptive) return AdaptiveUiState()
        return persistentPreferences.fold(AdaptiveUiState(isAdaptiveMode = true)) { state, modification ->
            when (modification) {
                UiModification.UIM01_TEXT -> state.copy(uim01Level = AdaptationLevel.LEVEL_2_MODERATE_SUPPORT)
                UiModification.UIM02_CONTEXTUAL_HELP_STEP_BY_STEP -> state.copy(uim02Level = AdaptationLevel.LEVEL_2_MODERATE_SUPPORT)
                UiModification.UIM03_REINFORCED_CONFIRMATION -> state.copy(uim03Level = AdaptationLevel.LEVEL_2_MODERATE_SUPPORT)
                UiModification.UIM04_VISUAL_FEEDBACK -> state.copy(uim04Level = AdaptationLevel.LEVEL_2_MODERATE_SUPPORT)
            }
        }
    }

    private fun getOrCreateTaskState(taskId: TaskId?, screenId: ScreenId?): TaskInteractionState {
        val key = Pair(taskId, screenId)
        return taskInteractionStates.getOrPut(key) {
            TaskInteractionState(
                taskId = taskId,
                screenId = screenId,
                screenEnteredAt = System.currentTimeMillis(),
                successfulActionAt = System.currentTimeMillis(),
                backCountInTask = 0,
                fieldErrorCount = 0,
                touchErrorCount = 0,
                helpRequestCount = 0,
                confirmationPause = false
            )
        }
    }

    private fun updateTaskState(taskId: TaskId?, screenId: ScreenId?, type: AdaptiveInteractionEventType): TaskInteractionState {
        val currentState = getOrCreateTaskState(taskId, screenId)
        val updatedState = when (type) {
            AdaptiveInteractionEventType.FIELD_ERROR -> currentState.copy(fieldErrorCount = currentState.fieldErrorCount + 1)
            AdaptiveInteractionEventType.TOUCH_ERROR -> currentState.copy(touchErrorCount = currentState.touchErrorCount + 1)
            AdaptiveInteractionEventType.BACK_PRESSED -> currentState.copy(backCountInTask = currentState.backCountInTask + 1)
            AdaptiveInteractionEventType.HELP_REQUESTED -> currentState.copy(helpRequestCount = currentState.helpRequestCount + 1)
            AdaptiveInteractionEventType.CONFIRMATION_PAUSE -> currentState.copy(confirmationPause = true)
            else -> currentState
        }
        taskInteractionStates[Pair(taskId, screenId)] = updatedState
        android.util.Log.i("AURA_MAPEK", "MONITOR | Updated TaskState: backCount=${updatedState.backCountInTask}, fieldErrors=${updatedState.fieldErrorCount}, touchErrors=${updatedState.touchErrorCount}, helpRequests=${updatedState.helpRequestCount}, confirmationPause=${updatedState.confirmationPause}")
        viewModelScope.launch { 
            if (taskId != null && screenId != null) {
                knowledge.saveTaskState(
                    TaskStateEntity(
                        taskId = taskId.name,
                        screenId = screenId.name,
                        touchErrorCount = updatedState.touchErrorCount,
                        fieldErrorCount = updatedState.fieldErrorCount,
                        helpRequestCount = updatedState.helpRequestCount,
                        backCount = updatedState.backCountInTask,
                        screenEnteredAt = updatedState.screenEnteredAt,
                        lastSuccessfulActionAt = updatedState.successfulActionAt,
                        confirmationPause = updatedState.confirmationPause
                    )
                )
            }
        }
        return updatedState
    }
    
    
    fun loadTaskState(taskId: TaskId, screenId: ScreenId) {
        viewModelScope.launch {
            val dbState = knowledge.getTaskState(taskId, screenId)
            if (dbState != null) {
                taskInteractionStates[Pair(taskId, screenId)] = TaskInteractionState(
                    taskId = taskId,
                    screenId = screenId,
                    screenEnteredAt = dbState.screenEnteredAt,
                    successfulActionAt = dbState.lastSuccessfulActionAt,
                    backCountInTask = dbState.backCount,
                    fieldErrorCount = dbState.fieldErrorCount,
                    touchErrorCount = dbState.touchErrorCount,
                    helpRequestCount = dbState.helpRequestCount,
                    confirmationPause = dbState.confirmationPause
                )
            }
        }
    }

    fun recordSuccessfulAction(taskId: TaskId?, screenId: ScreenId?) {
        val currentState = getOrCreateTaskState(taskId, screenId)
        taskInteractionStates[Pair(taskId, screenId)] = currentState.copy(successfulActionAt = System.currentTimeMillis())
    }

    fun processAdaptiveEvent(
        session: ExperimentSessionState?,
        taskId: TaskId?,
        screenId: ScreenId,
        type: AdaptiveInteractionEventType,
        reviewSummary: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary? = null
    ): Boolean {
        if (session == null) return false
        val interactionEntry = session.logEntry(type.toLogType(), taskId, screenId, "Interaction event: $type.")
        AppContainer.experimentLogger.log(interactionEntry)
        viewModelScope.launch { knowledge.saveInteractionEvent(interactionEntry, type.toOisCode()) }

        if (session.currentCondition != ExperimentCondition.SELF_ADAPTIVE_UI) return false

        android.util.Log.i("AURA_MAPEK", "EVENT | Received interaction event: $type on screen=$screenId for task=$taskId")
        val taskState = updateTaskState(taskId, screenId, type)

        val result = coordinator.process(
            event = AdaptiveInteractionEvent(taskId, screenId, type, System.currentTimeMillis(), reviewSummary),
            taskState = taskState,
            currentState = _uiState.value
        )

        return when (result) {
            AdaptationEngineResult.NoAdaptation -> false
            is AdaptationEngineResult.Suppressed -> {
                android.util.Log.i("AURA_MAPEK", "EVENT | Adaptation suppressed: rule=${result.ruleId} reason=${result.reason}")
                val previousLevel = when (result.ruleId) {
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR01_TIME_ON_SCREEN -> _uiState.value.uim01Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR02_BACKTRACKING -> _uiState.value.uim02Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR03_HELP_REQUEST -> _uiState.value.uim02Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR04_FIELD_ERROR -> _uiState.value.uim04Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> _uiState.value.uim03Level
                }
                val logEntry = session.logEntry(
                    eventType = InteractionEventType.ADAPTATION_SUPPRESSED,
                    taskId = taskId,
                    screenId = screenId,
                    message = "Suppressed ${result.ruleId}: ${result.reason}.",
                    adaptationRule = result.ruleId,
                    previousLevel = previousLevel,
                    newLevel = result.level,
                    userDecision = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision.NOT_REQUIRED
                )
                AppContainer.experimentLogger.log(logEntry)
                viewModelScope.launch {
                    knowledge.saveInteractionEvent(logEntry, null)
                }
                false
            }
            is AdaptationEngineResult.Applied -> {
                val snackbarMessage = when (result.plan.ruleId.name) {
                    "AR01_TIME_ON_SCREEN" -> "He aumentado el tamaño del texto para ayudarle a leer mejor."
                    "AR04_FIELD_ERROR" -> "He activado ayudas visuales para corregir el error."
                    else -> "He ajustado la interfaz para ayudarle."
                }
                _uiState.value = result.state.copy(isAdaptiveMode = true, snackbarMessage = snackbarMessage)
                
                val previousLevel = when (result.plan.ruleId) {
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR01_TIME_ON_SCREEN -> _uiState.value.uim01Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR02_BACKTRACKING -> _uiState.value.uim02Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR03_HELP_REQUEST -> _uiState.value.uim02Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR04_FIELD_ERROR -> _uiState.value.uim04Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> _uiState.value.uim03Level
                }

                val logEntry = session.logEntry(
                    eventType = InteractionEventType.ADAPTATION_APPLIED,
                    taskId = taskId,
                    screenId = screenId,
                    message = "Applied ${result.plan.ruleId}.",
                    observedSignal = result.plan.signals.firstOrNull(),
                    inferredDifficulty = result.plan.difficulties.firstOrNull(),
                    adaptationRule = result.plan.ruleId,
                    targetComponent = result.plan.modifications.firstOrNull(),
                    previousLevel = previousLevel,
                    newLevel = result.plan.targetLevel,
                    userDecision = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision.ACCEPTED
                )
                AppContainer.experimentLogger.log(logEntry)
                lastAppliedPlan = result.plan
                
                viewModelScope.launch {
                    knowledge.saveInteractionEvent(logEntry, null)
                    knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = true, userDecision = "ACCEPT"))
                }
                false
            }
            is AdaptationEngineResult.RequiresUserValidation -> {
                pendingPlan = result.plan
                _uiState.value = result.state.copy(isAdaptiveMode = true)
                
                val previousLevel = when (result.plan.ruleId) {
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR01_TIME_ON_SCREEN -> _uiState.value.uim01Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR02_BACKTRACKING -> _uiState.value.uim02Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR03_HELP_REQUEST -> _uiState.value.uim02Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR04_FIELD_ERROR -> _uiState.value.uim04Level
                    com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> _uiState.value.uim03Level
                }

                val logEntry = session.logEntry(
                    eventType = InteractionEventType.ADAPTATION_SUGGESTED,
                    taskId = taskId,
                    screenId = screenId,
                    message = "Suggested ${result.plan.ruleId}.",
                    observedSignal = result.plan.signals.firstOrNull(),
                    inferredDifficulty = result.plan.difficulties.firstOrNull(),
                    adaptationRule = result.plan.ruleId,
                    targetComponent = result.plan.modifications.firstOrNull(),
                    previousLevel = previousLevel,
                    newLevel = result.plan.targetLevel,
                    userDecision = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision.NOT_REQUIRED
                )
                AppContainer.experimentLogger.log(logEntry)
                viewModelScope.launch {
                    knowledge.saveInteractionEvent(logEntry, null)
                    knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = false, userDecision = null))
                }
                true
            }
        }
    }

    fun applyPendingAdaptation(session: ExperimentSessionState?) {
        if (session == null) return
        val plan = pendingPlan ?: return
        
        val previousLevel = when (plan.ruleId) {
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR01_TIME_ON_SCREEN -> _uiState.value.uim01Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR02_BACKTRACKING -> _uiState.value.uim02Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR03_HELP_REQUEST -> _uiState.value.uim02Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR04_FIELD_ERROR -> _uiState.value.uim04Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> _uiState.value.uim03Level
        }

        _uiState.value = coordinator.apply(plan, _uiState.value).copy(isAdaptiveMode = true)
        android.util.Log.i("AURA_MAPEK", "USER | Applied adaptation: rule=${plan.ruleId} targetLevel=${plan.targetLevel}")
        rememberPersistentPreferences(plan)
        lastAppliedPlan = plan
        
        val logEntry = session.logEntry(
            eventType = InteractionEventType.ADAPTATION_APPLIED,
            taskId = plan.taskId,
            screenId = plan.screenId,
            message = "Applied ${plan.ruleId}.",
            observedSignal = plan.signals.firstOrNull(),
            inferredDifficulty = plan.difficulties.firstOrNull(),
            adaptationRule = plan.ruleId,
            targetComponent = plan.modifications.firstOrNull(),
            previousLevel = previousLevel,
            newLevel = plan.targetLevel,
            userDecision = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision.ACCEPTED
        )
        AppContainer.experimentLogger.log(logEntry)
        viewModelScope.launch {
            knowledge.saveInteractionEvent(logEntry, null)
            knowledge.saveAdaptationEvent(plan.toEntity(session, applied = true, userDecision = "APPLY"))
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "APPLY"))
        }
        pendingPlan = null
    }

    fun rejectPendingAdaptation(session: ExperimentSessionState?) {
        if (session == null) return
        val plan = pendingPlan ?: return
        coordinator.reject(plan)
        android.util.Log.i("AURA_MAPEK", "USER | Rejected adaptation: rule=${plan.ruleId} targetLevel=${plan.targetLevel}")
        _uiState.value = _uiState.value.copy(
            isAdaptiveMode = true,
            pendingAdaptation = null,
            uim02Level = if (plan.modifications.contains(UiModification.UIM02_CONTEXTUAL_HELP_STEP_BY_STEP)) AdaptationLevel.LEVEL_2_MODERATE_SUPPORT else _uiState.value.uim02Level,
            contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea."
        )
        
        val logEntry = session.logEntry(
            eventType = InteractionEventType.ADAPTATION_REJECTED,
            taskId = plan.taskId,
            screenId = plan.screenId,
            message = "Rejected ${plan.ruleId}.",
            observedSignal = plan.signals.firstOrNull(),
            inferredDifficulty = plan.difficulties.firstOrNull(),
            adaptationRule = plan.ruleId,
            targetComponent = plan.modifications.firstOrNull(),
            previousLevel = plan.targetLevel,
            newLevel = plan.targetLevel,
            userDecision = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision.REJECTED
        )
        AppContainer.experimentLogger.log(logEntry)
        viewModelScope.launch {
            knowledge.saveInteractionEvent(logEntry, null)
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "REJECT"))
        }
        pendingPlan = null
    }

    fun undoAdaptation(session: ExperimentSessionState?) {
        if (session == null) return
        val plan = lastAppliedPlan ?: return
        
        val restoredState = coordinator.undo(plan, _uiState.value)
        android.util.Log.i("AURA_MAPEK", "USER | Undid/Restored adaptation: rule=${plan.ruleId} to level=${when (plan.ruleId) {
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR01_TIME_ON_SCREEN -> restoredState.uim01Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR02_BACKTRACKING -> restoredState.uim02Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR03_HELP_REQUEST -> restoredState.uim02Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR04_FIELD_ERROR -> restoredState.uim04Level
            com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> restoredState.uim03Level
        }}")
        _uiState.value = restoredState.copy(isAdaptiveMode = true)
        
        val logEntry = session.logEntry(
            eventType = InteractionEventType.ADAPTATION_UNDONE,
            taskId = plan.taskId,
            screenId = plan.screenId,
            message = "Undid ${plan.ruleId}.",
            observedSignal = plan.signals.firstOrNull(),
            inferredDifficulty = plan.difficulties.firstOrNull(),
            adaptationRule = plan.ruleId,
            targetComponent = plan.modifications.firstOrNull(),
            previousLevel = plan.targetLevel,
            newLevel = when (plan.ruleId) {
                com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR01_TIME_ON_SCREEN -> restoredState.uim01Level
                com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR02_BACKTRACKING -> restoredState.uim02Level
                com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR03_HELP_REQUEST -> restoredState.uim02Level
                com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR04_FIELD_ERROR -> restoredState.uim04Level
                com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId.AR05_CONFIRMATION_PAUSE -> restoredState.uim03Level
            },
            userDecision = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision.UNDONE
        )
        AppContainer.experimentLogger.log(logEntry)
        viewModelScope.launch {
            knowledge.saveInteractionEvent(logEntry, null)
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "UNDO"))
            knowledge.markUndone(plan.adaptationEventId)
        }
        lastAppliedPlan = null
    }

    fun keepLastAdaptation() {
        lastAppliedPlan?.let(::rememberPersistentPreferences)
        _uiState.update { it.copy(contextualHelpVisibleOverride = false, undoMessageVisible = false, pendingAdaptation = null) }
    }

    fun rememberAcceptedPersistentPreference(modification: UiModification) {
        if (modification == UiModification.UIM01_TEXT) {
            persistentPreferences.add(modification)
            _uiState.value = baselineState(_uiState.value.isAdaptiveMode)
        }
    }

    fun hideHelp() {
        _uiState.update { it.copy(contextualHelpVisibleOverride = false, undoMessageVisible = false, pendingAdaptation = null) }
    }

    private fun rememberPersistentPreferences(plan: AdaptationPlan) {
        val persistent = plan.modifications.filter { it == UiModification.UIM01_TEXT }
        persistentPreferences.addAll(persistent)
    }
}

private fun ExperimentSessionState.logEntry(
    eventType: InteractionEventType,
    taskId: TaskId? = null,
    screenId: ScreenId? = null,
    message: String,
    observedSignal: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ObservedInteractionSignal? = null,
    inferredDifficulty: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.InferredDifficulty? = null,
    adaptationRule: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId? = null,
    targetComponent: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification? = null,
    previousLevel: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel? = null,
    newLevel: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel? = null,
    userDecision: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UserDecision? = null,
    metadata: Map<String, String> = emptyMap()
): com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry {
    return com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry(
        participantId = participantId,
        group = group,
        condition = currentCondition,
        taskId = taskId,
        screenId = screenId,
        eventType = eventType,
        message = message,
        observedSignal = observedSignal,
        inferredDifficulty = inferredDifficulty,
        adaptationRule = adaptationRule,
        targetComponent = targetComponent,
        previousLevel = previousLevel,
        newLevel = newLevel,
        userDecision = userDecision,
        metadata = metadata
    )
}

private fun AdaptiveInteractionEventType.toLogType(): InteractionEventType {
    return when (this) {
        AdaptiveInteractionEventType.TOUCH_ERROR -> InteractionEventType.TOUCH_ERROR
        AdaptiveInteractionEventType.PROLONGED_TIME -> InteractionEventType.LONG_TIME_TRIGGERED
        AdaptiveInteractionEventType.CONFIRMATION_PAUSE -> InteractionEventType.CONFIRMATION_PAUSE
        AdaptiveInteractionEventType.BACK_PRESSED -> InteractionEventType.BACK_PRESSED
        AdaptiveInteractionEventType.HELP_REQUESTED -> InteractionEventType.HELP_REQUESTED
        AdaptiveInteractionEventType.FIELD_ERROR -> InteractionEventType.FIELD_ERROR
        AdaptiveInteractionEventType.ADAPTATION_REJECTED -> InteractionEventType.ADAPTATION_REJECTED
        AdaptiveInteractionEventType.SENSITIVE_ACTION -> InteractionEventType.SENSITIVE_ACTION
    }
}

private fun AdaptiveInteractionEventType.toOisCode(): String {
    return when (this) {
        AdaptiveInteractionEventType.TOUCH_ERROR -> "OIS04_FIELD_ERROR"
        AdaptiveInteractionEventType.PROLONGED_TIME -> "OIS01_TIME_ON_SCREEN"
        AdaptiveInteractionEventType.CONFIRMATION_PAUSE -> "OIS05_CONFIRMATION_PAUSE"
        AdaptiveInteractionEventType.BACK_PRESSED -> "OIS02_BACKTRACKING"
        AdaptiveInteractionEventType.HELP_REQUESTED -> "OIS03_HELP_REQUEST"
        AdaptiveInteractionEventType.FIELD_ERROR -> "OIS04_FIELD_ERROR"
        AdaptiveInteractionEventType.ADAPTATION_REJECTED -> "OIS03_HELP_REQUEST"
        AdaptiveInteractionEventType.SENSITIVE_ACTION -> "OIS05_CONFIRMATION_PAUSE"
    }
}

private fun AdaptationPlan.toEntity(
    session: ExperimentSessionState,
    applied: Boolean,
    userDecision: String?
): AdaptationEventEntity {
    return AdaptationEventEntity(
        adaptationEventId = adaptationEventId,
        sessionId = session.sessionId,
        participantId = session.participantId,
        condition = session.currentCondition.name,
        taskId = taskId?.name,
        screenId = screenId?.name,
        ruleId = ruleId.name,
        inferredDifficulty = difficulties.joinToString(",") { it.name },
        uiModifications = modifications.joinToString(",") { it.name },
        validationType = validationType.name,
        systemDecision = "PLAN_CREATED",
        userDecision = userDecision,
        applied = applied,
        undone = false,
        timestamp = System.currentTimeMillis()
    )
}

private fun AdaptationPlan.toDecisionEntity(
    session: ExperimentSessionState,
    decision: String
): UserDecisionEventEntity {
    return UserDecisionEventEntity(
        decisionId = UUID.randomUUID().toString(),
        adaptationEventId = null,
        sessionId = session.sessionId,
        participantId = session.participantId,
        taskId = taskId?.name,
        screenId = screenId?.name,
        decision = decision,
        timestamp = System.currentTimeMillis()
    )
}
