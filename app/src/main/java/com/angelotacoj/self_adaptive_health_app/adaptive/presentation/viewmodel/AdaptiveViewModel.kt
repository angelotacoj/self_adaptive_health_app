package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptationEngineResult
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.ExtendedMapeKCoordinator
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
                UiModification.UIM01_TEXT_SIZE -> state.copy(textScale = 1.25f)
                UiModification.UIM02_CONTRAST -> state.copy(highContrast = true)
                UiModification.UIM03_TOUCH_TARGETS -> state.copy(enlargedTouchTargets = true)
                UiModification.UIM04_SPACING -> state.copy(increasedSpacing = true)
                UiModification.UIM05_ICONS_LABELS -> state.copy(showIconLabels = true)
                UiModification.UIM10_SAFE_EXIT -> state.copy(safeExitEnabled = true)
                UiModification.UIM06_CONTEXTUAL_HELP,
                UiModification.UIM07_GUIDED_NAVIGATION,
                UiModification.UIM08_REINFORCED_CONFIRMATION,
                UiModification.UIM09_VISUAL_FEEDBACK -> state
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

        val taskState = updateTaskState(taskId, screenId, type)

        val result = coordinator.process(
            event = AdaptiveInteractionEvent(taskId, screenId, type, System.currentTimeMillis(), reviewSummary),
            taskState = taskState,
            currentState = _uiState.value
        )

        return when (result) {
            AdaptationEngineResult.NoAdaptation -> false
            is AdaptationEngineResult.Applied -> {
                val snackbarMessage = when (result.plan.ruleId.name) {
                    "AR02" -> "He aumentado el tamaño del texto para ayudarle a leer mejor."
                    "AR06" -> "He activado ayudas visuales para corregir el error."
                    else -> "He ajustado la interfaz para ayudarle."
                }
                _uiState.value = result.state.copy(isAdaptiveMode = true, snackbarMessage = snackbarMessage)
                lastAppliedPlan = result.plan
                
                val logEntry = session.logEntry(InteractionEventType.ADAPTATION_APPLIED, taskId, screenId, "Applied ${result.plan.ruleId}.")
                AppContainer.experimentLogger.log(logEntry)
                viewModelScope.launch {
                    knowledge.saveInteractionEvent(logEntry, null)
                    knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = true, userDecision = null))
                }
                false
            }
            is AdaptationEngineResult.RequiresUserValidation -> {
                pendingPlan = result.plan
                _uiState.value = result.state.copy(isAdaptiveMode = true)
                
                val logEntry = session.logEntry(InteractionEventType.ADAPTATION_SUGGESTED, taskId, screenId, "Suggested ${result.plan.ruleId}.")
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
        _uiState.value = coordinator.apply(plan, _uiState.value).copy(isAdaptiveMode = true)
        rememberPersistentPreferences(plan)
        lastAppliedPlan = plan
        
        val logEntry = session.logEntry(InteractionEventType.ADAPTATION_APPLIED, plan.taskId, plan.screenId, "Applied ${plan.ruleId}.")
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
        _uiState.value = _uiState.value.copy(
            isAdaptiveMode = true,
            pendingAdaptation = null,
            contextualHelpVisible = true,
            contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea."
        )
        
        val logEntry = session.logEntry(InteractionEventType.ADAPTATION_REJECTED, plan.taskId, plan.screenId, "Rejected ${plan.ruleId}.")
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
        
        _uiState.value = coordinator.undo(plan, _uiState.value).copy(isAdaptiveMode = true)
        
        val logEntry = session.logEntry(InteractionEventType.ADAPTATION_UNDONE, plan.taskId, plan.screenId, "Undid ${plan.ruleId}.")
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
        _uiState.update { it.copy(contextualHelpVisible = false, undoMessageVisible = false, pendingAdaptation = null) }
    }

    fun rememberAcceptedPersistentPreference(modification: UiModification) {
        if (modification == UiModification.UIM01_TEXT_SIZE) {
            persistentPreferences.add(modification)
            _uiState.value = baselineState(_uiState.value.isAdaptiveMode)
        }
    }

    fun hideHelp() {
        _uiState.update { it.copy(contextualHelpVisible = false, undoMessageVisible = false, pendingAdaptation = null) }
    }

    private fun rememberPersistentPreferences(plan: AdaptationPlan) {
        val persistent = plan.modifications.filter { it == UiModification.UIM01_TEXT_SIZE }
        persistentPreferences.addAll(persistent)
    }
}

private fun ExperimentSessionState.logEntry(
    eventType: InteractionEventType,
    taskId: TaskId? = null,
    screenId: ScreenId? = null,
    message: String,
    metadata: Map<String, String> = emptyMap()
): com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry {
    return com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry(
        participantCode = participantCode,
        group = group,
        condition = currentCondition,
        taskId = taskId,
        screenId = screenId,
        eventType = eventType,
        message = message,
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
        AdaptiveInteractionEventType.TOUCH_ERROR -> "OIS01_TOUCH_ERRORS"
        AdaptiveInteractionEventType.PROLONGED_TIME -> "OIS02_PROLONGED_TIME"
        AdaptiveInteractionEventType.CONFIRMATION_PAUSE -> "OIS03_CONFIRMATION_PAUSE"
        AdaptiveInteractionEventType.BACK_PRESSED -> "OIS04_BACKTRACKING"
        AdaptiveInteractionEventType.HELP_REQUESTED -> "OIS05_HELP_REQUEST"
        AdaptiveInteractionEventType.FIELD_ERROR -> "OIS06_FIELD_ERROR"
        AdaptiveInteractionEventType.ADAPTATION_REJECTED -> "OIS07_ADAPTATION_REJECTED"
        AdaptiveInteractionEventType.SENSITIVE_ACTION -> "OIS08_SENSITIVE_ACTION"
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
        participantCode = session.participantCode,
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
        participantCode = session.participantCode,
        taskId = taskId?.name,
        screenId = screenId?.name,
        decision = decision,
        timestamp = System.currentTimeMillis()
    )
}
