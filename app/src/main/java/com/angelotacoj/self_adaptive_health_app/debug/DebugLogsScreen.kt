package com.angelotacoj.self_adaptive_health_app.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ExperimentLogger
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugLogsScreen(
    logger: ExperimentLogger,
    session: ExperimentSessionState?,
    onDeleteCurrentSession: () -> Unit,
    onDeleteAllResearchData: () -> Unit,
    onBack: () -> Unit
) {
    var logs by remember { mutableStateOf(logger.getLogs().asReversed()) }
    var roomRows by remember { mutableStateOf<List<String>>(emptyList()) }
    var counts by remember { mutableStateOf("Cargando conteos...") }
    var confirmDeleteCurrent by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    suspend fun refreshRoomSummary() {
        val dao = AppContainer.database.experimentDao()
        val activeSessionId = session?.sessionId
        val interactions = if (activeSessionId != null) {
            dao.recentInteractionEventsForSession(activeSessionId)
        } else {
            dao.recentInteractionEvents()
        }
        val adaptations = if (activeSessionId != null) {
            dao.recentAdaptationEventsForSession(activeSessionId)
        } else {
            dao.recentAdaptationEvents()
        }
        val decisions = if (activeSessionId != null) {
            dao.recentUserDecisionEventsForSession(activeSessionId)
        } else {
            dao.recentUserDecisionEvents()
        }
        roomRows = interactions.map {
            "${formatTimestamp(it.timestamp)} - ${it.eventType} - ${it.condition} - ${it.taskId ?: "Ninguna"} - ${it.screenId ?: "Ninguna"} - ${it.oisCode ?: ""} - ${it.message}"
        } + adaptations.map {
            "${formatTimestamp(it.timestamp)} - ${it.ruleId} - ${it.condition} - ${it.taskId ?: "Ninguna"} - ${it.screenId ?: "Ninguna"} - ${it.userDecision ?: it.systemDecision}"
        } + decisions.map {
            "${formatTimestamp(it.timestamp)} - DECISION - ${it.taskId ?: "Ninguna"} - ${it.screenId ?: "Ninguna"} - ${it.decision}"
        }
        val globalCounts = "Histórico total: Sesiones: ${dao.participantSessionCount()} | Tareas: ${dao.taskRunCount()} | Interacciones: ${dao.interactionEventCount()} | Adaptaciones: ${dao.adaptationEventCount()} | Decisiones: ${dao.userDecisionEventCount()}"
        counts = if (activeSessionId != null) {
            "Sesión actual: Tareas: ${dao.taskRunCountForSession(activeSessionId)} | Interacciones: ${dao.interactionEventCountForSession(activeSessionId)} | Adaptaciones: ${dao.adaptationEventCountForSession(activeSessionId)} | Decisiones: ${dao.userDecisionEventCountForSession(activeSessionId)}\n$globalCounts"
        } else {
            globalCounts
        }
    }

    LaunchedEffect(session?.sessionId) {
        refreshRoomSummary()
    }

    if (confirmDeleteCurrent) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCurrent = false },
            title = { Text("Borrar sesión actual") },
            text = { Text("Esta acción eliminará los registros de la sesión actual en este dispositivo.") },
            confirmButton = {
                LargeDestructiveButton("Borrar sesión actual", {
                    confirmDeleteCurrent = false
                    onDeleteCurrentSession()
                })
            },
            dismissButton = { LargeSecondaryButton("Cancelar", { confirmDeleteCurrent = false }) }
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Borrar todos los registros") },
            text = { Text("Esta acción eliminará todos los registros de investigación de este dispositivo.") },
            confirmButton = {
                LargeDestructiveButton("Borrar todos los registros", {
                    confirmDeleteAll = false
                    onDeleteAllResearchData()
                })
            },
            dismissButton = { LargeSecondaryButton("Cancelar", { confirmDeleteAll = false }) }
        )
    }

    ScreenContainer(
        title = "Registros de depuración",
        subtitle = if (session != null) {
            "Vista de sesión actual. Los eventos listados pertenecen al participante activo."
        } else {
            "Vista histórica global. No hay participante activo seleccionado."
        },
        showNotice = false,
        navigationLabel = "Volver",
        onNavigationClick = onBack
    ) {
        //LargeSecondaryButton(text = "Volver al inicio", onClick = onBack)

        ResearcherSectionCard(if (session != null) "Resumen de sesión actual" else "Alcance del panel") {
            Text(text = if (session != null) "Modo: sesión actual filtrada por participante activo" else "Modo: histórico global del dispositivo")
            Text(text = "Participante: ${session?.participantCode ?: "Ninguno"}")
            Text(text = "Session ID: ${session?.sessionId ?: "No aplica"}")
            Text(text = "Grupo: ${session?.group?.label ?: "Ninguno"}")
            Text(text = "Condición actual: ${session?.currentCondition ?: "Ninguna"}")
            Text(text = "Total completado: ${session?.completedTasksByCondition?.values?.sumOf { it.size } ?: 0}/8")
        }

        ResearcherSectionCard("Registro en DB") {
            Text(text = counts)
        }

        ResearcherSectionCard("Estado de tareas por condición") {
            ConditionTaskStatus("STATIC_UI", session, ExperimentCondition.STATIC_UI)
            ConditionTaskStatus("SELF_ADAPTIVE_UI", session, ExperimentCondition.SELF_ADAPTIVE_UI)
        }

        if (logs.isEmpty() && roomRows.isEmpty()) {
            ResearcherSectionCard("Eventos recientes") {
                Text(text = "No hay registros todavía.")
            }
        } else {
            if (roomRows.isNotEmpty()) {
                ResearcherSectionCard(if (session != null) "Eventos persistidos de la sesión actual" else "Eventos persistidos recientes globales") {
                    roomRows.forEach { row ->
                        Text(text = row)
                    }
                }
            }
            logs.forEach { entry ->
                LogCard(entry = entry)
            }
        }

        LargeDestructiveButton("Borrar sesión actual", { confirmDeleteCurrent = true })
        LargeDestructiveButton("Borrar todos los registros", { confirmDeleteAll = true })
    }
}

@Composable
private fun ResearcherSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ConditionTaskStatus(
    title: String,
    session: ExperimentSessionState?,
    condition: ExperimentCondition
) {
    val completed = session?.completedTasksByCondition?.get(condition).orEmpty()
    Text(text = title, fontWeight = FontWeight.SemiBold)
    listOf(
        TaskId.T1_ACCESS,
        TaskId.T2_WELL_BEING,
        TaskId.T3_REMINDER,
        TaskId.T4_SUMMARY
    ).forEach { task ->
        val status = if (task in completed) "Completada" else "Pendiente"
        Text(text = "${task.label}: $status")
    }
}

@Composable
private fun LogCard(entry: DebugLogEntry) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${formatTimestamp(entry.timestamp)} - ${entry.eventType}",
                fontWeight = FontWeight.Bold
            )
            Text(text = "Tarea: ${entry.taskId?.label ?: "Ninguna"}")
            Text(text = "Pantalla: ${entry.screenId ?: "Ninguna"}")
            Text(text = entry.message)
            if (entry.metadata.isNotEmpty()) {
                Text(text = "Metadatos: ${entry.metadata.entries.joinToString { "${it.key}=${it.value}" }}")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.forLanguageTag("es-PE")).format(Date(timestamp))
}
