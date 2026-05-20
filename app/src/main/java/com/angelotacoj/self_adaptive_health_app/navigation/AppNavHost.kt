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
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.AdaptiveSnackbar
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ExperimentLogger
import com.angelotacoj.self_adaptive_health_app.core.logging.InteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.MapeKLog
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.conditionOrder
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskRunEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    var sessionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    val adaptiveUiStateFlow = remember { MutableStateFlow(AdaptiveUiState()) }
    val adaptiveUiState by adaptiveUiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingPlan by remember { mutableStateOf<AdaptationPlan?>(null) }
    var lastAppliedPlan by remember { mutableStateOf<AdaptationPlan?>(null) }
    var existingSetupSession by remember { mutableStateOf<ExperimentSessionState?>(null) }
    var pendingSetupSession by remember { mutableStateOf<com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession?>(null) }
    var conditionTransitionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    var sessionCompletedState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    val logger = AppContainer.experimentLogger
    val scope = rememberCoroutineScope()
    val knowledge = AppContainer.knowledgeRepository
    val engine = remember { ExtendedMapeKEngine(knowledge) }

    LaunchedEffect(adaptiveUiState.snackbarMessage) {
        adaptiveUiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            adaptiveUiStateFlow.value = adaptiveUiStateFlow.value.copy(snackbarMessage = null)
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
        adaptiveUiStateFlow.value = AdaptiveUiState(isAdaptiveMode = started.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
        pendingPlan = null
        lastAppliedPlan = null
        engine.clearEvents()
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
            adaptiveUiStateFlow.value = AdaptiveUiState(isAdaptiveMode = restored.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
            MapeKLog.experiment("active session restored session=${restored.sessionId} condition=${restored.currentCondition}")
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
        MapeKLog.stage("EVENT", "received eventType=$type task=$taskId screen=$screenId condition=${session.currentCondition}")
        MapeKLog.stage("EVENT", "$taskId | screen=$screenId | event=$type")
        MapeKLog.stage(
            "CONDITION",
            "current=${session.currentCondition} adaptationEnabled=${session.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI}"
        )
        MapeKLog.stage("MONITOR", "interaction event -> ${type} condition=${session.currentCondition} task=$taskId screen=$screenId")
        scope.launch { knowledge.saveInteractionEvent(interactionEntry, oisCode = type.toOisCode()) }
        if (session.currentCondition != ExperimentCondition.SELF_ADAPTIVE_UI) {
            MapeKLog.stage("STATE", "STATIC_UI logged event=${type}; adaptation skipped and AdaptiveUiState unchanged")
            return false
        }

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
                val snackbarMessage = when (result.plan.ruleId.name) {
                    "AR02" -> "He aumentado el tamaño del texto para ayudarle a leer mejor."
                    "AR06" -> "He activado ayudas visuales para corregir el error."
                    else -> "He ajustado la interfaz para ayudarle."
                }
                adaptiveUiStateFlow.value = result.state.copy(isAdaptiveMode = true, snackbarMessage = snackbarMessage)
                lastAppliedPlan = result.plan
                MapeKLog.stage("EXECUTOR", "undo target stored AR=${result.plan.ruleId} source=AUTO_APPLIED")
                MapeKLog.state("adaptiveUiState updated textScale=${result.state.textScale} contextualHelpVisible=${result.state.contextualHelpVisible}")
                log(session.logEntry(InteractionEventType.ADAPTATION_APPLIED, taskId, screenId, "Applied ${result.plan.ruleId}."))
                scope.launch {
                    knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = true, userDecision = null))
                    MapeKLog.stage("KNOWLEDGE", "knowledge save adaptation AR=${result.plan.ruleId} applied=true decision=null")
                    MapeKLog.stage("KNOWLEDGE", "saved adaptationEvent rule=${result.plan.ruleId} applied=true userDecision=null")
                    MapeKLog.stage("STATE", result.plan.trace("AUTO_APPLIED", "SAVED"))
                    MapeKLog.stage("STATE", "adaptiveUiState updated screen=$screenId rule=${result.plan.ruleId}")
                }
                false
            }
            is AdaptationEngineResult.RequiresUserValidation -> {
                pendingPlan = result.plan
                adaptiveUiStateFlow.value = result.state.copy(isAdaptiveMode = true)
                log(session.logEntry(InteractionEventType.ADAPTATION_SUGGESTED, taskId, screenId, "Suggested ${result.plan.ruleId}."))
                scope.launch {
                    knowledge.saveAdaptationEvent(result.plan.toEntity(session, applied = false, userDecision = null))
                    MapeKLog.stage("KNOWLEDGE", "knowledge save adaptation AR=${result.plan.ruleId} applied=false decision=pending")
                    MapeKLog.stage("KNOWLEDGE", "saved adaptationEvent rule=${result.plan.ruleId} applied=false userDecision=null")
                    MapeKLog.stage("STATE", result.plan.trace("PENDING_USER_VALIDATION", "SAVED"))
                    MapeKLog.stage("STATE", "adaptiveUiState updated screen=$screenId rule=${result.plan.ruleId}")
                }
                true
            }
        }
    }

    fun applyPendingAdaptation() {
        val session = activeSession() ?: return
        val plan = pendingPlan ?: return
        adaptiveUiStateFlow.value = engine.apply(plan, adaptiveUiState).copy(isAdaptiveMode = true)
        lastAppliedPlan = plan
        MapeKLog.stage("USER_VALIDATION", "validation decision=APPLY AR=${plan.ruleId}")
        log(session.logEntry(InteractionEventType.ADAPTATION_APPLIED, plan.taskId, plan.screenId, "Applied ${plan.ruleId}."))
        scope.launch {
            knowledge.saveAdaptationEvent(plan.toEntity(session, applied = true, userDecision = "APPLY"))
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "APPLY"))
            MapeKLog.stage("KNOWLEDGE", "knowledge save adaptation AR=${plan.ruleId} applied=true decision=APPLY")
            MapeKLog.stage("KNOWLEDGE", "saved adaptationEvent rule=${plan.ruleId} applied=true userDecision=APPLY")
            MapeKLog.stage("STATE", plan.trace("APPLY", "SAVED"))
            MapeKLog.stage("STATE", "adaptiveUiState updated screen=${plan.screenId} rule=${plan.ruleId}")
        }
        pendingPlan = null
    }

    fun rejectPendingAdaptation() {
        val session = activeSession() ?: return
        val plan = pendingPlan ?: return
        engine.reject(plan)
        adaptiveUiStateFlow.value = adaptiveUiState.copy(
            isAdaptiveMode = true,
            pendingAdaptation = null,
            contextualHelpVisible = true,
            contextualHelpMessage = "Entendido. No volveré a mostrar esta sugerencia durante esta tarea."
        )
        log(session.logEntry(InteractionEventType.ADAPTATION_REJECTED, plan.taskId, plan.screenId, "Rejected ${plan.ruleId}."))
        scope.launch {
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "REJECT"))
            MapeKLog.stage("KNOWLEDGE", "knowledge save user decision AR=${plan.ruleId} decision=REJECT")
            MapeKLog.stage("STATE", plan.trace("REJECT", "SAVED"))
        }
        pendingPlan = null
    }

    fun undoAdaptation() {
        val session = activeSession() ?: return
        val plan = lastAppliedPlan ?: run {
            MapeKLog.stage("EXECUTOR", "undo requested but no lastAppliedPlan is available")
            MapeKLog.state("undo ignored because no applied adaptation is stored")
            return
        }
        MapeKLog.stage("EXECUTOR", "undo requested AR=${plan.ruleId} currentTextScale=${adaptiveUiState.textScale}")
        adaptiveUiStateFlow.value = engine.undo(plan, adaptiveUiState).copy(isAdaptiveMode = true)
        log(session.logEntry(InteractionEventType.ADAPTATION_UNDONE, plan.taskId, plan.screenId, "Undid ${plan.ruleId}."))
        scope.launch {
            knowledge.saveUserDecision(plan.toDecisionEntity(session, "UNDO"))
            MapeKLog.stage("KNOWLEDGE", "knowledge save user decision AR=${plan.ruleId} decision=UNDO")
            MapeKLog.stage("STATE", plan.trace("UNDO", "SAVED"))
        }
        lastAppliedPlan = null
    }

    fun hideHelp() {
        adaptiveUiStateFlow.value = adaptiveUiState.copy(contextualHelpVisible = false, undoMessageVisible = false, pendingAdaptation = null)
        MapeKLog.stage("STATE", "AdaptiveUiState changed by hideHelp; Compose observes StateFlow and recomposes")
    }

    fun completeTask(taskId: TaskId, screenId: ScreenId, message: String) {
        val session = activeSession() ?: return
        val completedTaskState = session.finishCurrentTask()
        val requiredTasks = setOf(TaskId.T1_ACCESS, TaskId.T2_APPOINTMENT, TaskId.T3_WELL_BEING, TaskId.T4_REMINDER, TaskId.T5_SUMMARY)
        val completedForCondition = completedTaskState.completedTasksByCondition[session.currentCondition].orEmpty()
        val conditionDone = completedForCondition.containsAll(requiredTasks)
        val totalCompleted = completedTaskState.completedTasksByCondition.values.sumOf { it.size }
        val finalDone = conditionDone && totalCompleted >= 10
        sessionState = completedTaskState
        log(session.logEntry(InteractionEventType.TASK_COMPLETED, taskId, screenId, message))
        if (conditionDone) {
            log(session.logEntry(InteractionEventType.CONDITION_COMPLETED, screenId = screenId, message = "${session.currentCondition} condition completed."))
            MapeKLog.experiment("condition completed condition=${session.currentCondition}")
        }
        scope.launch {
            AppContainer.database.experimentDao().markTaskCompleted(
                sessionId = session.sessionId,
                condition = session.currentCondition.name,
                taskId = taskId.name,
                endedAt = System.currentTimeMillis()
            )
            AppContainer.experimentPreferences.saveSession(completedTaskState)
            MapeKLog.experiment("total completed tasks=${AppContainer.database.experimentDao().getTotalCompletedTaskCount(session.sessionId)}/10")
        }
        if (conditionDone && !finalDone && completedTaskState.currentConditionIndex < completedTaskState.conditionOrder.lastIndex) {
            conditionTransitionState = completedTaskState
            navController.navigate(AppRoute.ConditionTransition.route)
        }
        if (finalDone) {
            val finished = completedTaskState.finishSession()
            sessionState = finished
            sessionCompletedState = finished
            scope.launch {
                AppContainer.database.experimentDao().markParticipantSessionEnded(finished.sessionId, System.currentTimeMillis(), true)
                AppContainer.experimentPreferences.saveSession(finished)
            }
            MapeKLog.experiment("session completed total=10/10")
            navController.navigate(AppRoute.SessionCompleted.route)
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
            MapeKLog.experiment("total completed tasks=$total/8")
            if (completed || total >= 8) return@launch
            val started = session.startTask(taskId)
            dao.insertTaskRun(session.taskRun(taskId))
            withContext(Dispatchers.Main) {
                engine.clearEvents(taskId)
                knowledge.clearTask(taskId)
                sessionState = started
                log(session.logEntry(InteractionEventType.TASK_STARTED, taskId, ScreenId.HOME, message))
                navController.navigate(route)
            }
        }
    }

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
                    "Ya existe una sesión para ${it.participantCode}. Condición actual: ${it.currentCondition}. Tareas completadas: ${it.completedTasksByCondition.values.sumOf { tasks -> tasks.size }}/10."
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
                        adaptiveUiStateFlow.value = AdaptiveUiState(isAdaptiveMode = it.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
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
                        adaptiveUiStateFlow.value = AdaptiveUiState()
                        MapeKLog.state("adaptive state reset")
                        pendingPlan = null
                        lastAppliedPlan = null
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
                        onAdaptiveEvent = { type, screen -> processAdaptiveEvent(TaskId.T5_SUMMARY, screen, type) },
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
                            adaptiveUiStateFlow.value = AdaptiveUiState(isAdaptiveMode = next.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
                            pendingPlan = null
                            lastAppliedPlan = null
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
                        listOf("Total completado: ${completed.completedTasksByCondition.values.sumOf { it.size }}/10")
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
                            adaptiveUiStateFlow.value = AdaptiveUiState()
                            pendingPlan = null
                            lastAppliedPlan = null
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
                        adaptiveUiStateFlow.value = AdaptiveUiState()
                        pendingPlan = null
                        lastAppliedPlan = null
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

private fun AdaptationPlan.toEntity(
    session: ExperimentSessionState,
    applied: Boolean,
    userDecision: String?
): AdaptationEventEntity {
    return AdaptationEventEntity(
        adaptationEventId = UUID.randomUUID().toString(),
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

private fun AdaptationPlan.trace(validationDecision: String, knowledgeSave: String): String {
    return "trace=interaction event -> OIS=${signals.joinToString(prefix = "[", postfix = "]") { it.name }} -> DI=${difficulties.joinToString(prefix = "[", postfix = "]") { it.name }} -> AR=$ruleId -> validation decision=$validationDecision -> UI modification=${modifications.joinToString(prefix = "[", postfix = "]") { it.name }} -> knowledge save=$knowledgeSave"
}
