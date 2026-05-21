package com.angelotacoj.self_adaptive_health_app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine.ExtendedMapeKCoordinator
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.KnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.LocalAdaptiveEvent
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.AdaptiveSnackbar
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.viewmodel.AdaptiveViewModel
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ExperimentLogger
import com.angelotacoj.self_adaptive_health_app.core.logging.InteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.MapeKLog
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.model.completedTaskCount
import com.angelotacoj.self_adaptive_health_app.core.model.conditionOrder
import com.angelotacoj.self_adaptive_health_app.core.model.isCurrentConditionComplete
import com.angelotacoj.self_adaptive_health_app.core.model.isExperimentComplete
import com.angelotacoj.self_adaptive_health_app.core.model.totalRequiredTaskRuns
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskRunEntity
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.debug.DebugLogsScreen
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.experiment.ExperimentSetupScreen
import com.angelotacoj.self_adaptive_health_app.experiment.ExperimentSetupViewModel
import com.angelotacoj.self_adaptive_health_app.healthtasks.access.AccessAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.access.AccessEvent
import com.angelotacoj.self_adaptive_health_app.healthtasks.access.AccessScreen
import com.angelotacoj.self_adaptive_health_app.healthtasks.access.AccessViewModel
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
import com.angelotacoj.self_adaptive_health_app.ui.theme.Self_Adaptive_Health_AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AdaptiveViewModelFactory(
    private val coordinator: ExtendedMapeKCoordinator,
    private val knowledge: KnowledgeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdaptiveViewModel::class.java)) {
            "Unknown ViewModel class ${modelClass.name}"
        }
        return AdaptiveViewModel(coordinator, knowledge) as T
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    var sessionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    val knowledge = AppContainer.knowledgeRepository
    val engine = remember { ExtendedMapeKCoordinator(knowledge) }
    val adaptiveViewModel: AdaptiveViewModel = viewModel(factory = AdaptiveViewModelFactory(engine, knowledge))
    val adaptiveUiState by adaptiveViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var existingSetupSession by remember { mutableStateOf<ExperimentSessionState?>(null) }
    var pendingSetupSession by remember { mutableStateOf<com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession?>(null) }
    var conditionTransitionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    var sessionCompletedState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    val logger = AppContainer.experimentLogger
    val scope = rememberCoroutineScope()

    LaunchedEffect(adaptiveUiState.snackbarMessage) {
        adaptiveUiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            adaptiveViewModel.clearSnackbar()
        }
    }

    fun activeSession(): ExperimentSessionState? = sessionState?.takeIf { it.isSessionActive }

    fun adaptiveStateFor(session: ExperimentSessionState): AdaptiveUiState {
        return if (session.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI) {
            adaptiveUiState.copy(isAdaptiveMode = true)
        } else {
            AdaptiveUiState()
        }
    }

    fun log(entry: DebugLogEntry) {
        logger.log(entry)
        scope.launch {
            knowledge.saveInteractionEvent(entry, oisCode = null)
        }
    }

    suspend fun buildSessionStateFromRoom(sessionId: String): ExperimentSessionState? {
        val dao = AppContainer.database.experimentDao()
        val entity = dao.getSessionById(sessionId) ?: return null
        val group = runCatching { ExperimentGroup.valueOf(entity.group) }.getOrNull() ?: return null
        val dataSet = AppContainer.fakeHealthDataSource.getDataSet(group)
        val snapshot = AppContainer.experimentPreferences.sessionSnapshot.first()
        val isActiveSession = snapshot.isSessionActive && snapshot.currentSessionId == sessionId
        val conditionIndex = if (isActiveSession) {
            snapshot.currentConditionIndex.coerceIn(0, group.conditionOrder().lastIndex)
        } else {
            0
        }
        val taskRuns = dao.getTaskRunsForSession(sessionId)
        val completed = taskRuns
            .filter { it.completed }
            .mapNotNull { run ->
                val condition = runCatching { ExperimentCondition.valueOf(run.condition) }.getOrNull()
                val task = runCatching { TaskId.valueOf(run.taskId) }.getOrNull()
                if (condition != null && task != null) condition to task else null
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
        return ExperimentSessionState(
            participantCode = entity.participantCode,
            group = group,
            conditionOrder = group.conditionOrder(),
            currentConditionIndex = conditionIndex,
            currentDataSet = dataSet,
            completedTasksByCondition = completed,
            sessionStartedAt = entity.startedAt,
            isSessionActive = isActiveSession
        )
    }

    suspend fun restoreActiveSession(): ExperimentSessionState? {
        val snapshot = AppContainer.experimentPreferences.sessionSnapshot.first()
        if (!snapshot.isSessionActive || snapshot.currentSessionId == null) return null
        return buildSessionStateFromRoom(snapshot.currentSessionId)
    }

    fun startFreshSession(newSession: com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession) {
        val dataSet = AppContainer.fakeHealthDataSource.getDataSet(newSession.group)
        val started = ExperimentSessionState(
            participantCode = newSession.participantCode,
            group = newSession.group,
            conditionOrder = newSession.group.conditionOrder(),
            currentDataSet = dataSet
        )
        sessionState = started
        existingSetupSession = null
        pendingSetupSession = null
        adaptiveViewModel.setAdaptiveMode(started.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
        adaptiveViewModel.resetState()
        adaptiveViewModel.clearEvents()
        knowledge.clearCurrentTaskAdaptationMemory()
        scope.launch {
            AppContainer.experimentPreferences.saveSession(started)
            AppContainer.database.experimentDao().insertParticipantSession(
                ParticipantSessionEntity(
                    sessionId = started.sessionId,
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

    LaunchedEffect(Unit) {
        val restored = restoreActiveSession()
        if (restored != null) {
            sessionState = restored
            adaptiveViewModel.setAdaptiveMode(restored.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
            MapeKLog.experiment("active session restored session=${restored.sessionId} condition=${restored.currentCondition}")
        }
    }


    fun processAdaptiveEvent(taskId: TaskId?, screenId: ScreenId, type: AdaptiveInteractionEventType, summary: com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary? = null): Boolean {
        return adaptiveViewModel.processAdaptiveEvent(activeSession(), taskId, screenId, type, summary)
    }

    fun applyPendingAdaptation() {
        adaptiveViewModel.applyPendingAdaptation(activeSession())
    }

    fun rejectPendingAdaptation() {
        adaptiveViewModel.rejectPendingAdaptation(activeSession())
    }

    fun undoAdaptation() {
        adaptiveViewModel.undoAdaptation(activeSession())
    }

    fun hideHelp() {
        adaptiveViewModel.hideHelp()
    }


    fun completeTask(taskId: TaskId, screenId: ScreenId, message: String) {
        val session = activeSession() ?: return
        val completedTaskState = session.finishCurrentTask()
        val conditionDone = completedTaskState.isCurrentConditionComplete()
        val finalDone = completedTaskState.isExperimentComplete()
        val nextState = if (finalDone) completedTaskState.finishSession() else completedTaskState
        if (finalDone) {
            sessionCompletedState = nextState
        } else {
            sessionState = nextState
        }
        log(session.logEntry(InteractionEventType.TASK_COMPLETED, taskId, screenId, message))
        if (conditionDone) {
            log(session.logEntry(InteractionEventType.CONDITION_COMPLETED, screenId = screenId, message = "${session.currentCondition} condition completed."))
            MapeKLog.experiment("condition completed condition=${session.currentCondition}")
        }
        scope.launch {
            val dao = AppContainer.database.experimentDao()
            dao.markTaskCompleted(
                sessionId = session.sessionId,
                condition = session.currentCondition.name,
                taskId = taskId.name,
                endedAt = System.currentTimeMillis()
            )
            if (finalDone) {
                dao.markParticipantSessionEnded(nextState.sessionId, System.currentTimeMillis(), true)
            }
            AppContainer.experimentPreferences.saveSession(nextState)
            MapeKLog.experiment("total completed tasks=${dao.getTotalCompletedTaskCount(session.sessionId)}/${session.totalRequiredTaskRuns()}")
        }
        if (conditionDone && !finalDone && completedTaskState.currentConditionIndex < completedTaskState.conditionOrder.lastIndex) {
            conditionTransitionState = completedTaskState
            navController.navigate(AppRoute.ConditionTransition.route)
        } else if (finalDone) {
            MapeKLog.experiment("session completed total=${nextState.completedTaskCount()}/${nextState.totalRequiredTaskRuns()}")
            navController.navigate(AppRoute.SessionCompleted.route) {
                popUpTo(AppRoute.Home.route) { inclusive = true }
                launchSingleTop = true
            }
            sessionState = nextState
        }
    }

    fun startTaskIfAllowed(
        session: ExperimentSessionState,
        taskId: TaskId,
        route: String,
        message: String,
        navController: NavHostController
    ) {
        scope.launch {
            val dao = AppContainer.database.experimentDao()
            val completed = dao.isTaskCompleted(session.sessionId, session.currentCondition.name, taskId.name)
            MapeKLog.experiment("task completion validated from Room session=${session.sessionId} condition=${session.currentCondition} task=$taskId completed=$completed")
            val total = dao.getTotalCompletedTaskCount(session.sessionId)
            MapeKLog.experiment("total completed tasks=$total/${session.totalRequiredTaskRuns()}")
            if (completed || total >= session.totalRequiredTaskRuns()) return@launch
            val started = session.startTask(taskId)
            dao.insertTaskRun(session.taskRun(taskId))
            withContext(Dispatchers.Main) {
                adaptiveViewModel.clearEvents(taskId)
                knowledge.clearTask(taskId)
                sessionState = started
                log(session.logEntry(InteractionEventType.TASK_STARTED, taskId, ScreenId.HOME, message))
                navController.navigate(route)
            }
        }
    }

    Self_Adaptive_Health_AppTheme(highContrast = adaptiveUiState.highContrast) {
        CompositionLocalProvider(
            LocalAdaptiveEvent provides { type, screen, summary -> adaptiveViewModel.processAdaptiveEvent(activeSession(), null, screen, type, summary) }
        ) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.ExperimentSetup.route,
            modifier = Modifier.fillMaxSize()
        ) {
        composable(AppRoute.ExperimentSetup.route) {
            val viewModel: ExperimentSetupViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ExperimentSetupScreen(
                state = state,
                existingSessionMessage = existingSetupSession?.let {
                    "Ya existe una sesión para ${it.participantCode}. Condición actual: ${it.currentCondition}. Tareas completadas: ${it.completedTaskCount()}/${it.totalRequiredTaskRuns()}."
                },
                onAction = viewModel::onAction,
                onStartSession = { newSession ->
                    scope.launch {
                        val existing = AppContainer.database.experimentDao()
                            .getSessionByParticipantCode(newSession.participantCode)
                            .firstOrNull()
                        val active = existing?.let { buildSessionStateFromRoom(it.sessionId) }
                        if (active != null && active.isSessionActive) {
                            withContext(Dispatchers.Main) {
                                pendingSetupSession = newSession
                                existingSetupSession = active
                            }
                            MapeKLog.experiment("existing participant session found participant=${newSession.participantCode} session=${active.sessionId}")
                        } else {
                            withContext(Dispatchers.Main) {
                                startFreshSession(newSession)
                            }
                        }
                    }
                },
                onContinueExistingSession = {
                    existingSetupSession?.let {
                        sessionState = it
                        adaptiveViewModel.setAdaptiveMode(it.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
                        adaptiveViewModel.resetState()
                        scope.launch { AppContainer.experimentPreferences.saveSession(it) }
                        existingSetupSession = null
                        pendingSetupSession = null
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onStartNewSession = {
                    pendingSetupSession?.let { startFreshSession(it) }
                },
                onDeleteExistingSession = {
                    val existing = existingSetupSession
                    if (existing != null) {
                        scope.launch {
                            AppContainer.database.experimentDao().deleteSessionCascade(existing.sessionId)
                            AppContainer.experimentPreferences.clearActiveSessionPreferences()
                            MapeKLog.knowledge("delete current session sessionId=${existing.sessionId}")
                            existingSetupSession = null
                        }
                    }
                },
                onOpenResearcherPanel = {
                    navController.navigate(AppRoute.DebugLogs.route)
                }
            )
        }

        composable(AppRoute.Home.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: HomeViewModel = viewModel()
                val homeUiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(session.participantCode, session.currentCondition) {
                    log(session.logEntry(InteractionEventType.SCREEN_ENTERED, screenId = ScreenId.HOME, message = "Home screen entered."))
                }
                HomeScreen(
                    state = HomeState(session = session, dataSet = session.currentDataSet),
                    uiState = homeUiState,
                    events = viewModel.events,
                    onAction = viewModel::onAction,
                    onNavigateToAccess = {
                        startTaskIfAllowed(session, TaskId.T1_ACCESS, AppRoute.Access.route, "T1 access started.", navController)
                    },
                    onNavigateToAppointment = {
                        startTaskIfAllowed(session, TaskId.T2_APPOINTMENT, AppRoute.Appointments.route, "T2 appointment started.", navController)
                    },
                    onNavigateToWellBeing = {
                        startTaskIfAllowed(session, TaskId.T3_WELL_BEING, AppRoute.WellBeing.route, "T3 well-being started.", navController)
                    },
                    onNavigateToReminders = {
                        startTaskIfAllowed(session, TaskId.T4_REMINDER, AppRoute.Reminders.route, "T4 reminder started.", navController)
                    },
                    onNavigateToSummary = {
                        startTaskIfAllowed(session, TaskId.T5_SUMMARY, AppRoute.Summary.route, "T5 summary started.", navController)
                    },
                    onNavigateToDebugLogs = {
                        log(session.logEntry(InteractionEventType.BUTTON_CLICKED, screenId = ScreenId.HOME, message = "Debug logs opened."))
                        navController.navigate(AppRoute.DebugLogs.route)
                    },
                    onNavigateToSetup = {
                        MapeKLog.nav("cancel confirmed")
                        val cancelled = session.cancelSession()
                        log(cancelled.logEntry(InteractionEventType.SESSION_CANCELLED, screenId = ScreenId.HOME, message = "Session cancelled from Home."))
                        adaptiveViewModel.resetState()
                        MapeKLog.state("adaptive state reset")
                        scope.launch {
                            AppContainer.database.experimentDao().markParticipantSessionEnded(session.sessionId, System.currentTimeMillis(), false)
                            AppContainer.experimentPreferences.clearActiveSessionPreferences()
                            knowledge.clearCurrentTaskAdaptationMemory()
                            MapeKLog.experiment("active session cleared from DataStore")
                            sessionState = null
                        }
                        navController.navigate(AppRoute.ExperimentSetup.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                        MapeKLog.nav("navigate setup backStackCleared=true")
                    },
                    onHelpRequested = {
                        log(session.logEntry(InteractionEventType.HELP_REQUESTED, screenId = ScreenId.HOME, message = "Help requested from Home."))
                    }
                )
            }
        }

        composable(AppRoute.Access.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: AccessViewModel = viewModel()
                viewModel.start(session.currentDataSet.accessCredentials)
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(adaptiveUiState, session.currentCondition) {
                    viewModel.onAction(AccessAction.AdaptiveStateChanged(adaptiveStateFor(session)))
                }
                state?.let { state ->
                    AccessScreen(
                        state = state.copy(adaptiveUiState = adaptiveStateFor(session)),
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message -> log(session.logEntry(type, TaskId.T1_ACCESS, screen, message)) },
                        onFieldError = { screen, fieldId, errorType ->
                            log(
                                session.logEntry(
                                    eventType = InteractionEventType.FIELD_ERROR,
                                    taskId = TaskId.T1_ACCESS,
                                    screenId = screen,
                                    message = "Access validation field error.",
                                    metadata = mapOf(
                                        "fieldId" to fieldId,
                                        "valid" to "false",
                                        "errorType" to errorType,
                                        "valueStored" to "false"
                                    )
                                )
                            )
                        },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T1_ACCESS, screen, type) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onTaskCompleted = {
                            viewModel.finishTask()
                            completeTask(TaskId.T1_ACCESS, ScreenId.ACCESS_COMPLETED, "T1 access completed.")
                        },
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.Appointments.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: AppointmentViewModel = viewModel()
                viewModel.start(session.currentDataSet.appointment, session.currentDataSet.appointmentOptions)
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(adaptiveUiState, session.currentCondition) {
                    viewModel.onAction(AppointmentAction.AdaptiveStateChanged(adaptiveStateFor(session)))
                }
                state?.let { state ->
                    AppointmentScreen(
                        state = state.copy(adaptiveUiState = adaptiveStateFor(session)),
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message ->
                            log(session.logEntry(type, TaskId.T2_APPOINTMENT, screen, message))
                        },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T2_APPOINTMENT, screen, type) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onTaskCompleted = {
                            completeTask(TaskId.T2_APPOINTMENT, ScreenId.APPOINTMENT_COMPLETED, "T2 appointment completed.")
                        },
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )                }
            }
        }

        composable(AppRoute.WellBeing.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: WellBeingViewModel = viewModel()
                viewModel.start(session.currentDataSet.wellBeingRecord)
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(adaptiveUiState, session.currentCondition) {
                    viewModel.onAction(WellBeingAction.AdaptiveStateChanged(adaptiveStateFor(session)))
                }
                state?.let { state ->
                    WellBeingScreen(
                        state = state.copy(adaptiveUiState = adaptiveStateFor(session)),
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message ->
                            if (type == InteractionEventType.TASK_COMPLETED) {
                                completeTask(TaskId.T3_WELL_BEING, screen, message)
                            } else {
                                log(session.logEntry(type, TaskId.T3_WELL_BEING, screen, message))
                            }
                        },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T3_WELL_BEING, screen, type) },
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
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(adaptiveUiState, session.currentCondition) {
                    viewModel.onAction(ReminderAction.AdaptiveStateChanged(adaptiveStateFor(session)))
                }
                state?.let { state ->
                    ReminderScreen(
                        state = state.copy(adaptiveUiState = adaptiveStateFor(session)),
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message ->
                            if (type == InteractionEventType.TASK_COMPLETED) {
                                completeTask(TaskId.T4_REMINDER, screen, message)
                            } else {
                                log(session.logEntry(type, TaskId.T4_REMINDER, screen, message))
                            }
                        },
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T4_REMINDER, screen, type) },
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
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(adaptiveUiState, session.currentCondition) {
                    viewModel.onAction(SummaryAction.AdaptiveStateChanged(adaptiveStateFor(session)))
                }
                state?.let { state ->
                    SummaryScreen(
                        state = state.copy(adaptiveUiState = adaptiveStateFor(session)),
                        onAction = viewModel::onAction,
                        onLog = { type, screen, message ->
                            if (type == InteractionEventType.TASK_COMPLETED) {
                                completeTask(TaskId.T5_SUMMARY, screen, message)
                            } else {
                                log(session.logEntry(type, TaskId.T5_SUMMARY, screen, message))
                            }
                        },
                        onAdaptiveEvent = { type, screen, summary -> processAdaptiveEvent(TaskId.T5_SUMMARY, screen, type, summary) },
                        onApplyAdaptation = ::applyPendingAdaptation,
                        onRejectAdaptation = ::rejectPendingAdaptation,
                        onUndoAdaptation = ::undoAdaptation,
                        onHideHelp = ::hideHelp,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.ConditionTransition.route) {
            val completed = conditionTransitionState ?: activeSession() ?: returnToSetup(navController)
            if (completed != null) {
                ScreenContainer(
                    title = "Bloque de tareas completado",
                    subtitle = "Ha completado las tareas de esta etapa.",
                    showNotice = false
                ) {
                    InstructionCard(
                        "Administrar cuestionario",
                        listOf("Antes de continuar, el investigador debe aplicar el cuestionario UEQ-S correspondiente a la interfaz que acaba de usar.")
                    )
                    LargePrimaryButton(
                        "UEQ-S completado, continuar",
                        {
                            val next = completed.moveToNextCondition()
                            sessionState = next
                            conditionTransitionState = null
                            adaptiveViewModel.setAdaptiveMode(next.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
                            adaptiveViewModel.resetState()
                            scope.launch { AppContainer.experimentPreferences.saveSession(next) }
                            log(next.logEntry(InteractionEventType.CONDITION_STARTED, screenId = ScreenId.HOME, message = "Stage ${next.currentConditionIndex + 1} started."))
                            MapeKLog.experiment("moving to next condition next=${next.currentCondition}")
                            val returnedToHome = navController.popBackStack(AppRoute.Home.route, inclusive = false)
                            if (!returnedToHome) {
                                navController.navigate(AppRoute.Home.route) {
                                    popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        }
                    )
                }
            }
        }

        composable(AppRoute.SessionCompleted.route) {
            val completed = sessionCompletedState ?: sessionState
            ScreenContainer(
                title = "Sesión experimental completada",
                subtitle = "Sesión experimental completada.",
                showNotice = false
            ) {
                InstructionCard(
                    "Gracias",
                    listOf(
                        "El participante completó ambos bloques de tareas.",
                        "Ahora el investigador puede aplicar la entrevista o formulario cualitativo final."
                    )
                )
                LargePrimaryButton(
                    "Panel del investigador",
                    { navController.navigate(AppRoute.DebugLogs.route) }
                )
                if (completed != null) {
                    InstructionCard(
                        "Resumen",
                        listOf("Total completado: ${completed.completedTaskCount()}/${completed.totalRequiredTaskRuns()}")
                    )
                }
            }
        }

        composable(AppRoute.DebugLogs.route) {
            DebugLogsScreen(
                logger = logger,
                session = sessionState,
                onDeleteCurrentSession = {
                    val current = sessionState
                    if (current != null) {
                        scope.launch {
                            AppContainer.database.experimentDao().deleteSessionCascade(current.sessionId)
                            AppContainer.experimentPreferences.clearActiveSessionPreferences()
                            knowledge.clearCurrentTaskAdaptationMemory()
                            sessionState = null
                            adaptiveViewModel.resetState()
                            MapeKLog.knowledge("delete current session sessionId=${current.sessionId}")
                            navController.navigate(AppRoute.ExperimentSetup.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                },
                onDeleteAllResearchData = {
                    scope.launch {
                        AppContainer.database.experimentDao().clearAll()
                        AppContainer.experimentPreferences.clearActiveSessionPreferences()
                        knowledge.clearCurrentTaskAdaptationMemory()
                        logger.clear()
                        sessionState = null
                        adaptiveViewModel.resetState()
                        MapeKLog.knowledge("delete all research data completed")
                        navController.navigate(AppRoute.ExperimentSetup.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            snackbar = { data -> AdaptiveSnackbar(data) }
        )
    }
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

private fun ExperimentSessionState.taskRun(taskId: TaskId): TaskRunEntity {
    return TaskRunEntity(
        taskRunId = "${sessionId}_${taskId.name}_${System.currentTimeMillis()}",
        sessionId = sessionId,
        participantCode = participantCode,
        condition = currentCondition.name,
        taskId = taskId.name,
        dataSet = currentDataSet.id,
        startedAt = System.currentTimeMillis(),
        endedAt = null,
        completed = false
    )
}
