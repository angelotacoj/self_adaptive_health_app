package com.angelotacoj.self_adaptive_health_app.healthtasks.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.ui.HeroHeaderCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.SessionInfoCard
import com.angelotacoj.self_adaptive_health_app.core.ui.TaskCard

@Composable
fun HomeScreen(
    state: HomeState,
    uiState: HomeUiState,
    events: kotlinx.coroutines.flow.SharedFlow<HomeEvent>,
    onAction: (HomeAction) -> Unit,
    onNavigateToAccess: () -> Unit,
    onNavigateToWellBeing: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToDebugLogs: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onHelpRequested: () -> Unit
) {
    if (uiState.showCancelSessionConfirmation) {
        AlertDialog(
            onDismissRequest = { onAction(HomeAction.DismissCancelSessionClicked) },
            title = { Text("Cancelar sesión") },
            text = { Text("¿Desea cancelar la sesión experimental y volver a la configuración?") },
            confirmButton = {
                LargeDestructiveButton("Sí, cancelar sesión", { onAction(HomeAction.ConfirmCancelSessionClicked) })
            },
            dismissButton = {
                LargeSecondaryButton("No, continuar", { onAction(HomeAction.DismissCancelSessionClicked) })
            }
        )
    }
    if (uiState.showSessionHelp) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { onAction(HomeAction.DismissHelpClicked) },
            title = { Text("Ayuda de la sesión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Esta es una sesión experimental con datos simulados.")
                    Text("Debe completar cuatro tareas en la condición actual. Luego la aplicación le avisará cuándo continuar con la siguiente condición.")
                    Text("Puede usar Volver o Cancelar tarea si necesita regresar. No ingrese datos personales reales.")
                }
            },
            confirmButton = {
                LargeSecondaryButton("Entendido", { onAction(HomeAction.DismissHelpClicked) })
            }
        )
    }

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                HomeEvent.OpenAccess -> onNavigateToAccess()
                HomeEvent.OpenWellBeing -> onNavigateToWellBeing()
                HomeEvent.OpenReminders -> onNavigateToReminders()
                HomeEvent.OpenSummary -> onNavigateToSummary()
                HomeEvent.OpenDebugLogs -> onNavigateToDebugLogs()
                HomeEvent.HelpRequested -> onHelpRequested()
                HomeEvent.NavigateToSetup -> onNavigateToSetup()
            }
        }
    }

    ScreenContainer(
        title = "Inicio",
        subtitle = "Elija una tarea simulada. El texto y las acciones son grandes para facilitar la lectura."
    ) {
        HeroHeaderCard(
            appName = "Bienvenido a AURA",
            description = "Complete las tareas de salud simulada con calma. Puede volver o cancelar cuando lo necesite."
        )

        SessionInfoCard(
            participantCode = state.session.participantCode,
            group = state.session.group.label,
            condition = state.session.currentCondition.toString(),
            dataSet = state.dataSet.id
        )

        Text(
            text = "Tareas de salud simulada",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        TaskCard(
            title = "T1 Acceder con código/PIN simulado",
            description = "Use un código y PIN ficticios para acceder a la aplicación.",
            buttonText = state.buttonTextFor(TaskId.T1_ACCESS),
            onClick = { onAction(HomeAction.AccessTaskClicked) },
            status = state.statusFor(TaskId.T1_ACCESS),
            enabled = !state.isCompleted(TaskId.T1_ACCESS),
            startButtonTestTag = "start_t1_access"
        )
        TaskCard(
            title = "T2 Registro de bienestar",
            description = "Ingrese un valor simulado, valídelo y guarde el registro ficticio.",
            buttonText = state.buttonTextFor(TaskId.T2_WELL_BEING),
            onClick = { onAction(HomeAction.RegisterWellBeingClicked) },
            status = state.statusFor(TaskId.T2_WELL_BEING),
            enabled = !state.isCompleted(TaskId.T2_WELL_BEING),
            startButtonTestTag = "start_t2_wellbeing"
        )
        TaskCard(
            title = "T3 Recordatorio",
            description = "Configure un recordatorio ficticio seleccionando actividad, hora y frecuencia.",
            buttonText = state.buttonTextFor(TaskId.T3_REMINDER),
            onClick = { onAction(HomeAction.ConfigureReminderClicked) },
            status = state.statusFor(TaskId.T3_REMINDER),
            enabled = !state.isCompleted(TaskId.T3_REMINDER),
            startButtonTestTag = "start_t3_reminder"
        )
        TaskCard(
            title = "T4 Revisar y confirmar",
            description = "Revise el acceso, el registro y el recordatorio simulados; luego confirme, edite o cancele.",
            buttonText = state.buttonTextFor(TaskId.T4_SUMMARY),
            onClick = { onAction(HomeAction.ReviewInformationClicked) },
            status = state.statusFor(TaskId.T4_SUMMARY),
            enabled = !state.isCompleted(TaskId.T4_SUMMARY),
            startButtonTestTag = "start_t4_summary"
        )
        LargeSecondaryButton(
            text = "Ayuda: explicar esta sesión",
            onClick = { onAction(HomeAction.HelpClicked) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Herramientas del investigador", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LargeSecondaryButton(
                text = "Panel del investigador",
                onClick = { onAction(HomeAction.ViewDebugLogsClicked) }
            )
        }
        LargeDestructiveButton(
            text = "Cancelar sesión y volver a configuración",
            onClick = { onAction(HomeAction.CancelSessionClicked) }
        )
    }
}

private fun HomeState.statusFor(taskId: TaskId): String {
    return if (isCompleted(taskId)) "Completada" else "Pendiente"
}

private fun HomeState.buttonTextFor(taskId: TaskId): String {
    return if (isCompleted(taskId)) "Completada" else "Iniciar tarea"
}

private fun HomeState.isCompleted(taskId: TaskId): Boolean {
    val completed = session.completedTasksByCondition[session.currentCondition].orEmpty()
    return taskId in completed
}
