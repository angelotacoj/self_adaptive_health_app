package com.angelotacoj.self_adaptive_health_app.healthtasks.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.angelotacoj.self_adaptive_health_app.core.ui.HeroHeaderCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.SessionInfoCard
import com.angelotacoj.self_adaptive_health_app.core.ui.TaskCard

@Composable
fun HomeScreen(
    state: HomeState,
    events: kotlinx.coroutines.flow.SharedFlow<HomeEvent>,
    onAction: (HomeAction) -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToWellBeing: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToDebugLogs: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onHelpRequested: () -> Unit
) {
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                HomeEvent.OpenAppointments -> onNavigateToAppointments()
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
            title = "T1 Cita médica",
            description = "Consulte una cita ficticia, lea el detalle y confirme que fue revisada.",
            buttonText = "Iniciar tarea",
            onClick = { onAction(HomeAction.ConsultAppointmentClicked) },
        )
        TaskCard(
            title = "T2 Registro de bienestar",
            description = "Ingrese un valor simulado, valídelo y guarde el registro ficticio.",
            buttonText = "Iniciar tarea",
            onClick = { onAction(HomeAction.RegisterWellBeingClicked) },
        )
        TaskCard(
            title = "T3 Recordatorio",
            description = "Configure un recordatorio ficticio seleccionando actividad, hora y frecuencia.",
            buttonText = "Iniciar tarea",
            onClick = { onAction(HomeAction.ConfigureReminderClicked) },
        )
        TaskCard(
            title = "T4 Revisar y confirmar",
            description = "Revise la cita, el registro y el recordatorio simulados; luego confirme, edite o cancele.",
            buttonText = "Iniciar tarea",
            onClick = { onAction(HomeAction.ReviewInformationClicked) },
        )
        LargeSecondaryButton(
            text = "Ayuda: explicar esta sesión",
            onClick = { onAction(HomeAction.HelpClicked) }
        )
        LargeSecondaryButton(
            text = "Ver registros de depuración",
            onClick = { onAction(HomeAction.ViewDebugLogsClicked) }
        )
        LargeDestructiveButton(
            text = "Cancelar sesión y volver a configuración",
            onClick = { onAction(HomeAction.CancelSessionClicked) }
        )
    }
}
