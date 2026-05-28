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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.angelotacoj.self_adaptive_health_app.core.model.isTaskAvailableInCurrentCondition
import com.angelotacoj.self_adaptive_health_app.core.model.totalRequiredTaskRuns
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskRunEntity
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
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
import com.angelotacoj.self_adaptive_health_app.ueq.presentation.UeqEvent
import com.angelotacoj.self_adaptive_health_app.ueq.presentation.UeqScreen
import com.angelotacoj.self_adaptive_health_app.ueq.presentation.UeqViewModel
import com.angelotacoj.self_adaptive_health_app.interview.presentation.InterviewEvent
import com.angelotacoj.self_adaptive_health_app.interview.presentation.InterviewScreen
import com.angelotacoj.self_adaptive_health_app.interview.presentation.InterviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}



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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
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
    // Phase C1: track which condition the UEQ is being answered for
    var ueqConditionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
    // Phase C1.5: track session for short interview (shown after second UEQ)
    var interviewSessionState by remember { mutableStateOf<ExperimentSessionState?>(null) }
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
        // If datastore was wiped but we are resuming, we default to 0 and fix it during resume logic.
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
            participantId = entity.participantId,
            group = group,
            conditionOrder = group.conditionOrder(),
            currentConditionIndex = conditionIndex,
            currentDataSet = dataSet,
            completedTasksByCondition = completed,
            sessionStartedAt = entity.startedAt,
            isSessionActive = isActiveSession,
            isProfileCompleted = snapshot.isProfileCompleted
        ).let { s -> if (entity.isCompleted) s.finishSession() else s }
    }

    suspend fun restoreActiveSession(): ExperimentSessionState? {
        val snapshot = AppContainer.experimentPreferences.sessionSnapshot.first()
        if (!snapshot.isSessionActive || snapshot.currentSessionId == null) return null
        return buildSessionStateFromRoom(snapshot.currentSessionId)
    }

    fun startFreshSession(newSession: com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession) {
        val dataSet = AppContainer.fakeHealthDataSource.getDataSet(newSession.group)
        val started = ExperimentSessionState(
            participantId = newSession.participantId,
            // Phase C1.5: group is stored as legacy metadata only; conditionOrder() always returns FIXED_CONDITION_ORDER
            group = newSession.group,
            conditionOrder = newSession.group.conditionOrder(),  // returns STATIC -> ADAPTIVE for all groups
            currentDataSet = dataSet
        )
        sessionState = started
        existingSetupSession = null
        pendingSetupSession = null
        adaptiveViewModel.resetState()
        adaptiveViewModel.setAdaptiveMode(started.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
        adaptiveViewModel.clearEvents()
        knowledge.clearCurrentTaskAdaptationMemory()
        scope.launch {
            AppContainer.experimentPreferences.saveSession(started)
            AppContainer.database.experimentDao().insertParticipantSession(
                ParticipantSessionEntity(
                    sessionId = started.sessionId,
                    participantId = started.participantId,
                    group = started.group.name,
                    conditionOrder = started.conditionOrder.joinToString(",") { it.name },
                    startedAt = started.sessionStartedAt,
                    endedAt = null,
                    isCompleted = false
                )
            )
        }
        log(started.logEntry(InteractionEventType.FIXED_FLOW_STARTED, screenId = ScreenId.EXPERIMENT_SETUP, message = "Fixed-order flow started: STATIC → UEQ → ADAPTIVE → UEQ → Interview."))
        log(started.logEntry(InteractionEventType.SESSION_STARTED, screenId = ScreenId.EXPERIMENT_SETUP, message = "Experimental session started."))
        log(started.logEntry(InteractionEventType.CONDITION_STARTED, screenId = ScreenId.EXPERIMENT_SETUP, message = "${started.currentCondition} condition started."))
        navController.navigate(AppRoute.InitialProfile.route) {
            popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
            launchSingleTop = true
        }
    }


    suspend fun generateParticipantCode(suffix: String): String {
        val dao = AppContainer.database.experimentDao()
        var nextSequence = (dao.getMaxParticipantSequence() ?: 0) + 1
        while (true) {
            val candidate = "P%02d-%s".format(nextSequence, suffix)
            if (!dao.participantIdExists(candidate)) {
                MapeKLog.stage("PROFILE", "generated participantId=$candidate")
                return candidate
            }
            nextSequence += 1
        }
    }


    fun applyProfileToAdaptiveState(session: ExperimentSessionState) {
        adaptiveViewModel.resetTemporaryStateForTask(
            isAdaptive = session.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI
        )
        MapeKLog.experiment("adaptive state initialized from condition baseline condition=${session.currentCondition}")
    }

    LaunchedEffect(Unit) {
        val restored = restoreActiveSession()
        if (restored != null) {
            withContext(Dispatchers.Main) {
                sessionState = restored
                applyProfileToAdaptiveState(restored)
                MapeKLog.experiment("active session restored session=${restored.sessionId} condition=${restored.currentCondition}")
                if (activity == null || (!activity.isFinishing && !activity.isDestroyed)) {
                    if (restored.isProfileCompleted) {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(AppRoute.InitialProfile.route) {
                            popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        AppContainer.experimentPreferences.sessionSnapshot.collect { snapshot ->
            if (snapshot.currentSessionId == null && sessionState != null) {
                withContext(Dispatchers.Main) {
                    sessionState = null
                    existingSetupSession = null
                    pendingSetupSession = null
                    conditionTransitionState = null
                    sessionCompletedState = null
                    adaptiveViewModel.resetState()
                    knowledge.clearCurrentTaskAdaptationMemory()
                    if (activity == null || (!activity.isFinishing && !activity.isDestroyed)) {
                        navController.navigate(AppRoute.ExperimentSetup.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            }
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

    fun keepLastAdaptation() {
        adaptiveViewModel.keepLastAdaptation()
    }


    fun completeTask(taskId: TaskId, screenId: ScreenId, message: String) {
        val session = activeSession() ?: return
        val sessionForCompletion = if (session.currentTaskId == taskId) session else session.startTask(taskId)
        val completedTaskState = sessionForCompletion.finishCurrentTask()
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
        if (conditionDone) {
            ueqConditionState = completedTaskState
            if (!finalDone && completedTaskState.currentConditionIndex < completedTaskState.conditionOrder.lastIndex) {
                conditionTransitionState = completedTaskState
            } else {
                sessionCompletedState = completedTaskState
                MapeKLog.experiment("session completed total=${nextState.completedTaskCount()}/${nextState.totalRequiredTaskRuns()}")
            }
            navController.navigate(AppRoute.Ueq.route)
            if (finalDone) {
                sessionState = nextState
            }
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
            if (completed || total >= session.totalRequiredTaskRuns() || !session.isTaskAvailableInCurrentCondition(taskId)) return@launch
            val started = session.startTask(taskId)
            dao.insertTaskRun(session.taskRun(taskId))
            withContext(Dispatchers.Main) {
                adaptiveViewModel.resetTemporaryStateForTask(session.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
                adaptiveViewModel.clearEvents(taskId)
                knowledge.clearTask(taskId)
                sessionState = started
                log(session.logEntry(InteractionEventType.TASK_STARTED, taskId, ScreenId.HOME, message))
                navController.navigate(route)
            }
        }
    }

    Self_Adaptive_Health_AppTheme(highContrast = activeSession()?.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI && adaptiveUiState.highContrast) {
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
            var generatedParticipantCode by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(state.participantSuffix) {
                generatedParticipantCode = state.participantSuffix.takeIf { it.length == 4 }
            }
            ExperimentSetupScreen(
                state = state,
                generatedParticipantCode = generatedParticipantCode,
                existingSessionMessage = existingSetupSession?.let {
                    if (!it.isSessionActive && it.completedTaskCount() == it.totalRequiredTaskRuns()) {
                        "Ya existe una sesión completada para el código ${it.participantId}."
                    } else {
                        "Ya existe una sesión pendiente para este código (${it.participantId}). ¿Desea continuarla?"
                    }
                },
                onAction = viewModel::onAction,
                onStartSession = { newSession ->
                    scope.launch {
                        val code = newSession.participantId
                        val existingSessions = AppContainer.database.experimentDao()
                            .getSessionByParticipantCode(code)
                        val existingEntity = existingSessions.firstOrNull { !it.isCompleted } 
                            ?: existingSessions.firstOrNull()
                        
                        if (existingEntity != null) {
                            val active = buildSessionStateFromRoom(existingEntity.sessionId)
                            withContext(Dispatchers.Main) {
                                pendingSetupSession = newSession
                                existingSetupSession = active
                            }
                            MapeKLog.experiment("existing participant session found participant=$code session=${existingEntity.sessionId}")
                        } else {
                            withContext(Dispatchers.Main) {
                                startFreshSession(newSession)
                            }
                        }
                    }
                },
                onContinueExistingSession = {
                    existingSetupSession?.let { existingSession ->
                        scope.launch {
                            val dao = AppContainer.database.experimentDao()
                            val ueqDao = AppContainer.database.ueqDao()
                            val interviewDao = AppContainer.database.interviewDao()
                            val isProfileCompleted = dao.getInitialUserProfile(existingSession.sessionId) != null
                            val pendingStep = SessionResumeResolver.resolvePendingStep(
                                session = existingSession,
                                experimentDao = dao,
                                ueqDao = ueqDao,
                                interviewDao = interviewDao
                            )
                            
                            if (pendingStep.isUeq) {
                                ueqConditionState = existingSession
                            }
                            if (pendingStep.isInterview) {
                                interviewSessionState = existingSession
                            }
                            
                            val resumedSession = existingSession.copy(
                                isSessionActive = true,
                                isProfileCompleted = isProfileCompleted,
                                currentConditionIndex = pendingStep.targetConditionIndex
                            )
                            
                            withContext(Dispatchers.Main) {
                                sessionState = resumedSession
                                adaptiveViewModel.resetState()
                                adaptiveViewModel.setAdaptiveMode(resumedSession.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI)
                                existingSetupSession = null
                                pendingSetupSession = null
                            }
                            AppContainer.experimentPreferences.saveSession(resumedSession)
                            log(resumedSession.logEntry(InteractionEventType.SESSION_STARTED, screenId = ScreenId.EXPERIMENT_SETUP, message = "Session resumed from code ${resumedSession.participantId}."))
                            
                            withContext(Dispatchers.Main) {
                                navController.navigate(pendingStep.route) {
                                    popUpTo(AppRoute.ExperimentSetup.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
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
                            MapeKLog.stage("SECURITY", "delete requested type=CURRENT_SESSION")
                            MapeKLog.stage("SECURITY", "pin validation result=SUCCESS")
                            AppContainer.database.experimentDao().deleteSessionCascade(existing.sessionId)
                            val activeId = AppContainer.experimentPreferences.sessionSnapshot.first().currentSessionId
                            if (activeId == existing.sessionId) {
                                AppContainer.experimentPreferences.clearActiveSessionPreferences()
                            }
                            MapeKLog.knowledge("delete current session completed sessionId=${existing.sessionId}")
                            existingSetupSession = null
                        }
                    }
                },
                onOpenResearcherPanel = {
                    navController.navigate(AppRoute.DebugLogs.route)
                }
            )
        }

        composable(AppRoute.InitialProfile.route) {
            val session = sessionState ?: return@composable
            com.angelotacoj.self_adaptive_health_app.experiment.InitialProfileScreen { q1, q2, q3, q4, q5, q6, q7, q8 ->
                val entity = com.angelotacoj.self_adaptive_health_app.core.persistence.room.InitialUserProfileEntity(
                    sessionId = session.sessionId,
                    participantId = session.participantId,
                    prefersLargeText = q1,
                    prefersLargeButtons = q2,
                    prefersIconLabels = q3,
                    prefersGuidedSteps = q4,
                    prefersConfirmations = q5,
                    mobileComfortLevel = q6,
                    prefersErrorExamples = q7,
                    prefersAdaptationPrompt = q8,
                    timestamp = System.currentTimeMillis()
                )
                scope.launch {
                    knowledge.saveInitialUserProfile(entity)
                    MapeKLog.experiment("profile saved sessionId=${session.sessionId}")
                    MapeKLog.experiment("initial profile added to Knowledge Base")
                    adaptiveViewModel.resetTemporaryStateForTask(
                        isAdaptive = session.currentCondition == ExperimentCondition.SELF_ADAPTIVE_UI
                    )
                    MapeKLog.experiment("profile saved without pre-applying adaptive UI")
                    
                    val nextSession = session.copy(isProfileCompleted = true)
                    sessionState = nextSession
                    AppContainer.experimentPreferences.markProfileCompleted()
                    AppContainer.experimentPreferences.saveSession(nextSession)
                    withContext(Dispatchers.Main.immediate){
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.InitialProfile.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
        composable(AppRoute.Home.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: HomeViewModel = viewModel()
                val homeUiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(session.participantId, session.currentCondition) {
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
                    onNavigateToUeq = {
                        ueqConditionState = session
                        navController.navigate(AppRoute.Ueq.route)
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
                        onKeepAdaptation = ::keepLastAdaptation,
                        onTaskCompleted = {
                            val payload = org.json.JSONObject().apply {
                                put("participantCode", state.userCode)
                                put("simulatedAccessCompleted", true)
                                put("note", "No se usó una cuenta real ni un registro clínico real.")
                            }.toString()
                            scope.launch {
                                AppContainer.database.experimentDao().insertTaskOutput(
                                    com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity(
                                        participantId = session.participantId,
                                        sessionId = session.sessionId,
                                        condition = session.currentCondition.name,
                                        taskId = TaskId.T1_ACCESS.name,
                                        taskOutputType = "ACCESS",
                                        payloadJson = payload,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
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
                        onKeepAdaptation = ::keepLastAdaptation,
                        onTaskCompleted = {
                            val appointment = state.selectedAppointment
                            val payload = org.json.JSONObject().apply {
                                put("selectedAppointmentId", appointment?.title)
                                put("professionalName", appointment?.professionalName)
                                put("specialty", appointment?.specialty)
                                put("appointmentDate", appointment?.date)
                                put("appointmentTime", appointment?.time)
                                put("simulatedLocation", appointment?.location)
                                put("mainInstruction", appointment?.instruction)
                                put("preparationInstruction", appointment?.preparation)
                                put("itemsToBring", appointment?.itemsToBring)
                                put("accessibilityNote", appointment?.accessibilityNote)
                                put("simulationNote", "Esta es una simulación.")
                            }.toString()
                            scope.launch {
                                AppContainer.database.experimentDao().insertTaskOutput(
                                    com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity(
                                        participantId = session.participantId,
                                        sessionId = session.sessionId,
                                        condition = session.currentCondition.name,
                                        taskId = TaskId.T2_APPOINTMENT.name,
                                        taskOutputType = "APPOINTMENT",
                                        payloadJson = payload,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
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
                                val payload = org.json.JSONObject().apply {
                                    put("energyLevel", state.energyLevel)
                                    put("mood", state.mood)
                                    put("note", state.note)
                                    put("simulatedDate", java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.forLanguageTag("es-ES")).format(java.util.Date()))
                                    put("completedAt", System.currentTimeMillis())
                                    put("simulatedHealthData", true)
                                }.toString()
                                scope.launch {
                                    AppContainer.database.experimentDao().insertTaskOutput(
                                        com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity(
                                            participantId = session.participantId,
                                            sessionId = session.sessionId,
                                            condition = session.currentCondition.name,
                                            taskId = TaskId.T3_WELL_BEING.name,
                                            taskOutputType = "WELLBEING",
                                            payloadJson = payload,
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
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
                        onKeepAdaptation = ::keepLastAdaptation,
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
                                val payload = org.json.JSONObject().apply {
                                    put("reminderType", state.selectedType)
                                    put("reminderDate", state.selectedDate)
                                    put("reminderTime", state.selectedTime)
                                    put("frequency", state.selectedFrequency)
                                    put("location", state.optionalLocation)
                                    put("note", state.optionalNote)
                                    put("noRealNotification", true)
                                    put("completedAt", System.currentTimeMillis())
                                }.toString()
                                scope.launch {
                                    AppContainer.database.experimentDao().insertTaskOutput(
                                        com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity(
                                            participantId = session.participantId,
                                            sessionId = session.sessionId,
                                            condition = session.currentCondition.name,
                                            taskId = TaskId.T4_REMINDER.name,
                                            taskOutputType = "REMINDER",
                                            payloadJson = payload,
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
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
                        onKeepAdaptation = ::keepLastAdaptation,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.Summary.route) {
            val session = activeSession() ?: returnToSetup(navController)
            if (session != null) {
                val viewModel: SummaryViewModel = viewModel()
                var taskOutputs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Map<String, String>>(emptyMap()) }
                LaunchedEffect(session.sessionId, session.currentCondition) {
                    val outputs = AppContainer.database.experimentDao().getTaskOutputsForSession(
                        session.participantId, session.sessionId, session.currentCondition.name
                    )
                    taskOutputs = outputs.associate { it.taskId to it.payloadJson }
                }
                viewModel.start(taskOutputs)
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
                                val payload = org.json.JSONObject().apply {
                                    put("finalSimulatedInformationConfirmed", true)
                                    put("timestamp", System.currentTimeMillis())
                                }.toString()
                                scope.launch {
                                    AppContainer.database.experimentDao().insertTaskOutput(
                                        com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity(
                                            participantId = session.participantId,
                                            sessionId = session.sessionId,
                                            condition = session.currentCondition.name,
                                            taskId = TaskId.T5_SUMMARY.name,
                                            taskOutputType = "FINAL_REVIEW",
                                            payloadJson = payload,
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
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
                        onKeepAdaptation = ::keepLastAdaptation,
                        onExit = { navController.popBackStack(AppRoute.Home.route, inclusive = false) }
                    )
                }
            }
        }

        composable(AppRoute.ConditionTransition.route) {
            val completed = conditionTransitionState ?: activeSession() ?: returnToSetup(navController)
            if (completed != null) {
                ScreenContainer(
                    title = "Primera etapa completada",
                    subtitle = "Ha completado la primera interfaz y el cuestionario UEQ.",
                    showNotice = false
                ) {
                    InstructionCard(
                        "Continuar con la segunda etapa",
                        listOf(
                            "A continuación comenzará la segunda interfaz.",
                            "Cuando esté listo, pulse el botón para continuar."
                        )
                    )
                    LargePrimaryButton(
                        "Continuar con la segunda interfaz",
                        {
                            val next = completed.moveToNextCondition()
                            sessionState = next
                            conditionTransitionState = null
                            applyProfileToAdaptiveState(next)
                            scope.launch { AppContainer.experimentPreferences.saveSession(next) }
                            log(next.logEntry(InteractionEventType.CONDITION_STARTED, screenId = ScreenId.HOME, message = "Etapa ${next.currentConditionIndex + 1} iniciada después de pausa opcional."))
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
                    Spacer(Modifier.height(16.dp))
                    LargeSecondaryButton(
                        "Finalizar por ahora",
                        {
                            log(completed.logEntry(InteractionEventType.BUTTON_CLICKED, screenId = ScreenId.HOME, message = "Sesión pausada después de STATIC UEQ."))
                            sessionState = null
                            conditionTransitionState = null
                            scope.launch { AppContainer.experimentPreferences.clearActiveSessionPreferences() }
                            navController.navigate(AppRoute.ExperimentSetup.route) {
                                popUpTo(AppRoute.ExperimentSetup.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }

        composable(AppRoute.Ueq.route) {
            val ueqSession = ueqConditionState ?: activeSession() ?: returnToSetup(navController)
            if (ueqSession != null) {
                val viewModel: UeqViewModel = viewModel()
                val ueqState by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(ueqSession.sessionId, ueqSession.currentCondition) {
                    viewModel.init(
                        participantId = ueqSession.participantId,
                        sessionId = ueqSession.sessionId,
                        group = ueqSession.group,
                        condition = ueqSession.currentCondition,
                        onSaved = {
                            log(
                                ueqSession.logEntry(
                                    eventType = InteractionEventType.UEQ_SAVED,
                                    screenId = ScreenId.UEQ_QUESTIONNAIRE,
                                    message = "UEQ guardado para condición ${ueqSession.currentCondition}."
                                )
                            )
                        }
                    )
                    log(
                        ueqSession.logEntry(
                            eventType = InteractionEventType.UEQ_OPENED,
                            screenId = ScreenId.UEQ_QUESTIONNAIRE,
                            message = "UEQ abierto para condición ${ueqSession.currentCondition}."
                        )
                    )
                }

                // Phase C1.5: after final condition UEQ → Interview; after first condition UEQ → ConditionTransition
                LaunchedEffect(ueqState.isSaved) {
                    if (ueqState.isSaved) {
                        kotlinx.coroutines.delay(1500L)
                        val isFinal = ueqSession.currentConditionIndex == ueqSession.conditionOrder.lastIndex
                        if (isFinal) {
                            // Route to short interview after second UEQ
                            interviewSessionState = ueqSession
                            navController.navigate(AppRoute.Interview.route) {
                                popUpTo(AppRoute.Ueq.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(AppRoute.ConditionTransition.route) {
                                popUpTo(AppRoute.Ueq.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                UeqScreen(
                    state = ueqState,
                    onEvent = { event ->
                        if (event is UeqEvent.Submit && !ueqState.allAnswered) {
                            log(
                                ueqSession.logEntry(
                                    eventType = InteractionEventType.UEQ_INCOMPLETE_SUBMIT,
                                    screenId = ScreenId.UEQ_QUESTIONNAIRE,
                                    message = "Intento de envío incompleto: ${ueqState.answeredCount}/${ueqState.totalItems} respondidos."
                                )
                            )
                        }
                        viewModel.onEvent(event)
                    }
                )
            }
        }

        composable(AppRoute.Interview.route) {
            val interviewSession = interviewSessionState ?: activeSession() ?: returnToSetup(navController)
            if (interviewSession != null) {
                val viewModel: InterviewViewModel = viewModel()
                val interviewState by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(interviewSession.sessionId) {
                    viewModel.init(
                        participantId = interviewSession.participantId,
                        sessionId = interviewSession.sessionId,
                        onFinished = {
                            // onFinished is a callback – navigation happens in isSaved LaunchedEffect below
                        }
                    )
                    log(
                        interviewSession.logEntry(
                            eventType = InteractionEventType.INTERVIEW_OPENED,
                            screenId = ScreenId.INTERVIEW_SCREEN,
                            message = "Entrevista breve abierta para participante ${interviewSession.participantId}."
                        )
                    )
                }

                LaunchedEffect(interviewState.isSaved) {
                    if (interviewState.isSaved) {
                        // Determine whether it was saved or skipped
                        // (both set isSaved = true; skip path doesn't persist entities)
                        navController.navigate(AppRoute.SessionCompleted.route) {
                            popUpTo(AppRoute.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                InterviewScreen(
                    state = interviewState,
                    onEvent = { event ->
                        when (event) {
                            is InterviewEvent.Save -> {
                                log(
                                    interviewSession.logEntry(
                                        eventType = InteractionEventType.INTERVIEW_SAVED,
                                        screenId = ScreenId.INTERVIEW_SCREEN,
                                        message = "Entrevista guardada para participante ${interviewSession.participantId}."
                                    )
                                )
                            }
                            is InterviewEvent.Skip -> {
                                log(
                                    interviewSession.logEntry(
                                        eventType = InteractionEventType.INTERVIEW_SKIPPED,
                                        screenId = ScreenId.INTERVIEW_SCREEN,
                                        message = "Entrevista omitida para participante ${interviewSession.participantId}."
                                    )
                                )
                            }
                            else -> Unit
                        }
                        viewModel.onEvent(event)
                    }
                )
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
                        "El participante completó ambos bloques de tareas, los cuestionarios UEQ y la entrevista.",
                        "El investigador puede acceder al panel para revisar los datos registrados."
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
                            MapeKLog.stage("SECURITY", "delete requested type=CURRENT_SESSION")
                            MapeKLog.stage("SECURITY", "pin validation result=SUCCESS")
                            AppContainer.database.experimentDao().deleteSessionCascade(current.sessionId)
                            AppContainer.database.ueqDao().deleteResponsesForSession(current.sessionId)
                            AppContainer.database.interviewDao().deleteResponsesForSession(current.sessionId)
                            AppContainer.experimentPreferences.clearActiveSessionPreferences()
                            knowledge.clearCurrentTaskAdaptationMemory()
                            sessionState = null
                            adaptiveViewModel.resetState()
                            MapeKLog.knowledge("delete current session completed sessionId=${current.sessionId}")
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
                        MapeKLog.stage("SECURITY", "delete requested type=ALL_RESEARCH_DATA")
                        MapeKLog.stage("SECURITY", "pin validation result=SUCCESS")
                        AppContainer.database.experimentDao().deleteAllResearchData()
                        AppContainer.database.ueqDao().deleteAllUeqResponses()
                        AppContainer.database.interviewDao().deleteAllResponses()
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
    MapeKLog.nav("returnToSetup fallback triggered (session is null). Letting central LaunchedEffect handle redirection.")
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
        participantId = participantId,
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
        participantId = participantId,
        condition = currentCondition.name,
        taskId = taskId.name,
        dataSet = currentDataSet.id,
        startedAt = System.currentTimeMillis(),
        endedAt = null,
        completed = false
    )
}
