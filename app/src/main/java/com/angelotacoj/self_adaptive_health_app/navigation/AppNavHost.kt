package com.angelotacoj.self_adaptive_health_app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.AdaptationEngineResult
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.ExtendedMapeKEngine
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationPlan
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEvent
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.TaskInteractionState
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ExperimentLogger
import com.angelotacoj.self_adaptive_health_app.core.logging.InteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.model.conditionOrder
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskRunEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity
import com.angelotacoj.self_adaptive_health_app.debug.DebugLogsScreen
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.experiment.ExperimentSetupScreen
import com.angelotacoj.self_adaptive_health_app.experiment.ExperimentSetupViewModel
import com.angelotacoj.self_adaptive_health_app.healthtasks.appointments.AppointmentAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.appointments.AppointmentScreen
import com.angelotacoj.self_adaptive_health_app.healthtasks.appointments.AppointmentViewModel
import com.angelotacoj.self_adaptive_health_app.healthtasks.home.HomeAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.home.HomeScreen
import com.angelotacoj.self_adaptive_health_app.healthtasks.home.HomeState
import com.angelotacoj.self_adaptive_health_app.healthtasks.home.HomeViewModel
import com.angelotacoj.self_adaptive_health_app.healthtasks.reminders.ReminderAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.reminders.ReminderScreen
import com.angelotacoj.self_adaptive_health_app.healthtasks.reminders.ReminderViewModel
import com.angelotacoj.self_adaptive_health_app.healthtasks.summary.SummaryAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.summary.SummaryScreen
import com.angelotacoj.self_adaptive_health_app.healthtasks.summary.SummaryViewModel
import com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing.WellBeingAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing.WellBeingScreen
import com.angelotacoj.self_adaptive_health_app.healthtasks.wellbeing.WellBeingViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    var sessionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    var adaptiveUiState by remember { mutableStateOf(AdaptiveUiState()) }
    var pendingPlan by remember { mutableStateOf<AdaptationPlan?>(null) }
    var lastAppliedPlan by remember { mutableStateOf<AdaptationPlan?>(null) }
    val logger = AppContainer.experimentLogger
    val scope = rememberCoroutineScope()
    val knowledge = AppContainer.knowledgeRepository
    val engine = remember { ExtendedMapeKEngine(knowledge) }

    fun activeSession(): ExperimentSessionState? = sessionState?.takeIf { it.isSessionActive }

    fun log(entry: DebugLogEntry) {
        logger.log(entry)
        scope.launch {
            knowledge.saveInteractionEvent(entry, oisCode = null)
        }
    }

    fun processAdaptiveEvent(
        taskId: TaskId?,
        screenId: ScreenId,
        type: AdaptiveInteractionEventType
    ): Boolean {
        val session = activeSession() ?: return false
        val interactionEntry = session.logEntry(type.toLogType(), taskId, screenId, "Interaction event: $type.")
        logger.log(interactionEntry)
        scope.launch { knowledge.saveInteractionEvent(interactionEntry, oisCode = type.toOisCode()) }
        if (session.currentCondition != ExperimentCondition.SELF_ADAPTIVE_UI) return false

        val result = engine.process(
            event = AdaptiveInteractionEvent(taskId, screenId, type),
            taskState = TaskInteractionState(
                taskId = taskId,
                screenId = screenId,
                screenEnteredAt = System.currentTimeMillis(),
                successfulActionAt = System.currentTimeMillis(),
                backCountInTask = 0,
                fieldErrorCount = if (type == AdaptiveInteractionEventType.FIELD_ERROR) 1 else 0
            ),
            currentState = adaptiveUiState
        )
        return when (result) {
            AdaptationEngineResult.NoAdaptation -> false
            is AdaptationEngineResult.Applied -> {
                adaptiveUiState = result.state
                log(session.logEntry(InteractionEventType.ADAPTATION_APPLIED, taskId, screenId, "Applied ${result.plan.ruleId}."))
                scope.launch { knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = true, userDecision = null)) }
                false
            }
            is AdaptationEngineResult.RequiresUserValidation -> {
                pendingPlan = result.plan
                adaptiveUiState = result.state
                log(session.logEntry(InteractionEventType.ADAPTATION_SUGGESTED, taskId, screenId, "Suggested ${result.plan.ruleId}."))
                scope.launch { knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = false, userDecision = null)) }
                true
            }
        }
    }

    fun applyPendingAdaptation() {
        val session = activeSession() ?: return
        val plan = pendingPlan ?: return
        adaptiveUiState = engine.apply(plan, adaptiveUiState)
        lastAppliedPlan = plan
        log(session.logEntry(InteractionEventType.ADAPTATION_APPLIED, plan.taskId, plan.screenId, "Applied ${plan.ruleId}."))
        scope.launch {
            knowledge.saveAdaptationEvent(plan.toEntity(session, applied = true, userDecision = "APPLY"))
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "APPLY"))
        }
        pendingPlan = null
    }

    fun rejectPendingAdaptation() {
        val session = activeSession() ?: return
        val plan = pendingPlan ?: return
        engine.reject(plan)
        adaptiveUiState = adaptiveUiState.copy(
            pendingAdaptation = null,
            contextualHelpVisible = true,
            contextualHelpMessage = "Understood. I will not show this suggestion again during this task."
        )
        log(session.logEntry(InteractionEventType.ADAPTATION_REJECTED, plan.taskId, plan.screenId, "Rejected ${plan.ruleId}."))
        scope.launch { knowledge.saveUserDecision(plan.toDecisionEntity(session, "REJECT")) }
        pendingPlan = null
    }

    fun undoAdaptation() {
        val session = activeSession() ?: return
        val plan = lastAppliedPlan ?: return
        adaptiveUiState = engine.undo(plan, adaptiveUiState)
        log(session.logEntry(InteractionEventType.ADAPTATION_UNDONE, plan.taskId, plan.screenId, "Undid ${plan.ruleId}."))
        scope.launch { knowledge.saveUserDecision(plan.toDecisionEntity(session, "UNDO")) }
        lastAppliedPlan = null
    }

    fun hideHelp() {
        adaptiveUiState = adaptiveUiState.copy(contextualHelpVisible = false, undoMessageVisible = false, pendingAdaptation = null)
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.ExperimentSetup.route,
        modifier = Modifier
    ) {
        composable(AppRoute.ExperimentSetup.route) {
            val viewModel: ExperimentSetupViewModel = viewModel()
            ExperimentSetupScreen(
                state = viewModel.state,
                onAction = viewModel::onAction,
                onStartSession = { newSession ->
                    val dataSet = AppContainer.fakeHealthDataSource.getDataSet(newSession.group)
                    val started = ExperimentSessionState(
                        participantCode = newSession.participantCode,
                        group = newSession.group,
                        conditionOrder = newSession.group.conditionOrder(),
                        currentDataSet = dataSet
                    )
                    sessionState = started
                    adaptiveUiState = AdaptiveUiState()
                    pendingPlan = null
                    scope.launch {
                        AppContainer.experimentPreferences.saveSession(started)
                        AppContainer.database.experimentDao().insertParticipantSession(
                            ParticipantSessionEntity(
                                sessionId = started.sessionId(),
                                participantCode = started.participantCode,
                                group = started.group.name,
                                conditionOrder = started.conditionOrder.joinToString(",") { it.name },
                                startedAt = started.sessionStartedAt,
                                endedAt = null,
                                isCompleted = false
                            )
                        )
                    }
                    log(started.logEntry(InteractionEventType.SESSION_STARTED, screenId = ScreenId.EXPERIMENT_SETUP, message = "Experimental session started."))
                    log(started.logEntry(InteractionEventType.CONDITION_STARTED, screenId = ScreenId.EXPERIMENT_SETUP, message = "${started.currentCondition} condition started."))
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoute.Home.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: HomeViewModel = viewModel()
                LaunchedEffect(session.participantCode, session.currentCondition) {
                    log(session.logEntry(InteractionEventType.SCREEN_ENTERED, screenId = ScreenId.HOME, message = "Home screen entered."))
                }
                HomeScreen(
                    state = HomeState(session = session, dataSet = session.currentDataSet),
                    events = viewModel.events,
                    onAction = viewModel::onAction,
                    onNavigateToAppointments = {
                        sessionState = session.startTask(TaskId.T1_APPOINTMENT)
                        scope.launch { AppContainer.database.experimentDao().insertTaskRun(session.taskRun(TaskId.T1_APPOINTMENT)) }
                        log(session.logEntry(InteractionEventType.TASK_STARTED, TaskId.T1_APPOINTMENT, ScreenId.HOME, "T1 started."))
                        navController.navigate(AppRoute.Appointments.route)
                    },
                    onNavigateToWellBeing = {
                        sessionState = session.startTask(TaskId.T2_WELL_BEING)
                        scope.launch { AppContainer.database.experimentDao().insertTaskRun(session.taskRun(TaskId.T2_WELL_BEING)) }
                        log(session.logEntry(InteractionEventType.TASK_STARTED, TaskId.T2_WELL_BEING, ScreenId.HOME, "T2 started."))
                        navController.navigate(AppRoute.WellBeing.route)
                    },
                    onNavigateToReminders = {
                        sessionState = session.startTask(TaskId.T3_REMINDER)
                        scope.launch { AppContainer.database.experimentDao().insertTaskRun(session.taskRun(TaskId.T3_REMINDER)) }
                        log(session.logEntry(InteractionEventType.TASK_STARTED, TaskId.T3_REMINDER, ScreenId.HOME, "T3 started."))
                        navController.navigate(AppRoute.Reminders.route)
                    },
                    onNavigateToSummary = {
                        sessionState = session.startTask(TaskId.T4_SUMMARY)
                        scope.launch { AppContainer.database.experimentDao().insertTaskRun(session.taskRun(TaskId.T4_SUMMARY)) }
                        log(session.logEntry(InteractionEventType.TASK_STARTED, TaskId.T4_SUMMARY, ScreenId.HOME, "T4 started."))
                        navController.navigate(AppRoute.Summary.route)
                    },
                    onNavigateToDebugLogs = {
                        log(session.logEntry(InteractionEventType.BUTTON_CLICKED, screenId = ScreenId.HOME, message = "Debug logs opened."))
                        navController.navigate(AppRoute.DebugLogs.route)
                    },
                    onNavigateToSetup = {
                        val cancelled = session.cancelSession()
                        sessionState = null
                        adaptiveUiState = AdaptiveUiState()
                        pendingPlan = null
                        lastAppliedPlan = null
                        scope.launch { knowledge.clearCurrentSession() }
                        log(cancelled.logEntry(InteractionEventType.SESSION_CANCELLED, screenId = ScreenId.HOME, message = "Session cancelled from Home."))
                        navController.navigate(AppRoute.ExperimentSetup.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onHelpRequested = {
                        log(session.logEntry(InteractionEventType.HELP_REQUESTED, screenId = ScreenId.HOME, message = "Help requested from Home."))
                    }
                )
            }
        }

        composable(AppRoute.Appointments.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: AppointmentViewModel = viewModel()
                viewModel.start(session.currentDataSet.appointment, session.currentDataSet.appointmentOptions)
                LaunchedEffect(adaptiveUiState) { viewModel.onAction(AppointmentAction.AdaptiveStateChanged(adaptiveUiState)) }
                viewModel.state?.let { state ->
                    AppointmentScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message -> log(session.logEntry(type, TaskId.T1_APPOINTMENT, screen, message)) },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T1_APPOINTMENT, screen, type) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.WellBeing.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: WellBeingViewModel = viewModel()
                viewModel.start(session.currentDataSet.wellBeingRecord)
                LaunchedEffect(adaptiveUiState) { viewModel.onAction(WellBeingAction.AdaptiveStateChanged(adaptiveUiState)) }
                viewModel.state?.let { state ->
                    WellBeingScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message -> log(session.logEntry(type, TaskId.T2_WELL_BEING, screen, message)) },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T2_WELL_BEING, screen, type) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.Reminders.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: ReminderViewModel = viewModel()
                viewModel.start(session.currentDataSet.reminder)
                LaunchedEffect(adaptiveUiState) { viewModel.onAction(ReminderAction.AdaptiveStateChanged(adaptiveUiState)) }
                viewModel.state?.let { state ->
                    ReminderScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message -> log(session.logEntry(type, TaskId.T3_REMINDER, screen, message)) },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T3_REMINDER, screen, type) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.Summary.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: SummaryViewModel = viewModel()
                viewModel.start(session.currentDataSet)
                LaunchedEffect(adaptiveUiState) { viewModel.onAction(SummaryAction.AdaptiveStateChanged(adaptiveUiState)) }
                viewModel.state?.let { state ->
                    SummaryScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message -> log(session.logEntry(type, TaskId.T4_SUMMARY, screen, message)) },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T4_SUMMARY, screen, type) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.DebugLogs.route) {
            DebugLogsScreen(
                logger = logger,
                onBack = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
            )
        }
    }
}

private fun returnToSetup(navController: NavHostController): Nothing? {
    navController.navigate(AppRoute.ExperimentSetup.route) {
        popUpTo(navController.graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
    return null
}

private fun ExperimentSessionState.logEntry(
    eventType: InteractionEventType,
    taskId: TaskId? = null,
    screenId: ScreenId? = null,
    message: String,
    metadata: Map<String, String> = emptyMap()
): DebugLogEntry {
    return DebugLogEntry(
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

private fun ExperimentSessionState.sessionId(): String = "${participantCode}_${sessionStartedAt}"

private fun ExperimentSessionState.taskRun(taskId: TaskId): TaskRunEntity {
    return TaskRunEntity(
        taskRunId = "${sessionId()}_${taskId.name}_${System.currentTimeMillis()}",
        sessionId = sessionId(),
        participantCode = participantCode,
        condition = currentCondition.name,
        taskId = taskId.name,
        dataSet = currentDataSet.id,
        startedAt = System.currentTimeMillis(),
        endedAt = null,
        completed = false
    )
}

private fun AdaptationPlan.toEntity(
    session: ExperimentSessionState,
    applied: Boolean,
    userDecision: String?
): AdaptationEventEntity {
    return AdaptationEventEntity(
        adaptationEventId = UUID.randomUUID().toString(),
        sessionId = session.sessionId(),
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
        sessionId = session.sessionId(),
        participantCode = session.participantCode,
        taskId = taskId?.name,
        screenId = screenId?.name,
        decision = decision,
        timestamp = System.currentTimeMillis()
    )
}
