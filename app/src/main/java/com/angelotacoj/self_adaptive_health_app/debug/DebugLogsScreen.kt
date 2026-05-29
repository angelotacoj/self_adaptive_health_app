package com.angelotacoj.self_adaptive_health_app.debug

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentTaskOrder
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.model.completedTaskCount
import com.angelotacoj.self_adaptive_health_app.core.model.totalRequiredTaskRuns
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.security.ResearcherPinDialog
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.InitialUserProfileEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Status of the JSON export operation shown to the evaluator. */
private enum class ExportStatus { Idle, Exporting, Success, Failed }

@Composable
fun DebugLogsScreen(
    logger: ExperimentLogger,
    session: ExperimentSessionState?,
    onDeleteCurrentSession: () -> Unit,
    onDeleteAllResearchData: () -> Unit,
    onExportData: (
        suggestedFilename: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) -> Unit,
    onBack: () -> Unit
) {
    var logs by remember { mutableStateOf(logger.getLogs().asReversed()) }
    var roomRows by remember { mutableStateOf<List<String>>(emptyList()) }
    var counts by remember { mutableStateOf("Cargando conteos...") }
    var pendingDelete by remember { mutableStateOf<ResearcherDeleteAction?>(null) }
    var profile by remember { mutableStateOf<InitialUserProfileEntity?>(null) }
    var adaptationSummary by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var exportStatus by remember { mutableStateOf(ExportStatus.Idle) }

    suspend fun refreshRoomSummary() {
        val dao = AppContainer.database.experimentDao()
                val activeSessionId = session?.sessionId
        if (activeSessionId != null) {
            profile = dao.getInitialUserProfile(activeSessionId)
            
            // Adaptation summary by AR rule
            val ads = dao.recentAdaptationEventsForSession(activeSessionId)
            val summary = mutableMapOf<String, Int>()
            ads.forEach {
                if (it.applied) summary[it.ruleId] = (summary[it.ruleId] ?: 0) + 1
            }
            adaptationSummary = summary
        } else {
            profile = null
            adaptationSummary = emptyMap()
        }
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

    pendingDelete?.let { action ->
        ResearcherPinDialog(
            onConfirm = {
                pendingDelete = null
                when (action) {
                    ResearcherDeleteAction.CurrentSession -> onDeleteCurrentSession()
                    ResearcherDeleteAction.AllResearchData -> onDeleteAllResearchData()
                }
            },
            onCancel = { pendingDelete = null }
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
        if (session != null){
            ResearcherSectionCard(if (session != null) "Resumen de sesión actual" else "Alcance del panel") {
                Text(text = if (session != null) "Modo: sesión actual filtrada por participante activo" else "Modo: histórico global del dispositivo")
                Text(text = "Participante: ${session?.participantId ?: "Ninguno"}")
                Text(text = "Session ID: ${session?.sessionId ?: "No aplica"}")
                Text(text = "Grupo: ${session?.group?.label ?: "Ninguno"}")
                Text(text = "Condición actual: ${session?.currentCondition ?: "Ninguna"}")
                Text(text = "Estado: ${if (session?.isSessionActive == true) "Activa" else "Completada/Cancelada"}")
                Text(text = "Perfil completado: ${if (session?.isProfileCompleted == true) "Sí" else "No"}")
                Text(text = "Total completado: ${session?.completedTaskCount() ?: 0}/${session?.totalRequiredTaskRuns() ?: 0}")
            }


            ResearcherSectionCard("Resumen del perfil inicial") {
                if (profile != null) {
                    Text("Textos grandes: ${profile!!.prefersLargeText}")
                    Text("Botones grandes: ${profile!!.prefersLargeButtons}")
                    Text("Etiquetas de iconos: ${profile!!.prefersIconLabels}")
                    Text("Guía paso a paso: ${profile!!.prefersGuidedSteps}")
                    Text("Confirmaciones: ${profile!!.prefersConfirmations}")
                    Text("Comodidad móvil: ${profile!!.mobileComfortLevel}")
                    Text("Ejemplos de errores: ${profile!!.prefersErrorExamples}")
                    Text("Aviso de adaptación: ${profile!!.prefersAdaptationPrompt}")
                } else {
                    Text("No hay perfil registrado para esta sesión.")
                }
            }

            ResearcherSectionCard("Resumen de adaptaciones (Aplicadas)") {
                if (adaptationSummary.isEmpty()) {
                    Text("Ninguna adaptación aplicada.")
                } else {
                    adaptationSummary.forEach { (rule, count) ->
                        Text("$rule: $count vez/veces")
                    }
                }
            }
        }

        ResearcherSectionCard("Registro en DB") {
            Text(text = counts)
        }

        if (session != null){
            ResearcherSectionCard("Estado de tareas por condición") {
                ConditionTaskStatus("STATIC_UI", session, ExperimentCondition.STATIC_UI)
                ConditionTaskStatus("SELF_ADAPTIVE_UI", session, ExperimentCondition.SELF_ADAPTIVE_UI)
            }
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

        // ── Exportar datos del estudio ──────────────────────────────────────
        ResearcherSectionCard("Exportar datos") {
            Text(
                text = "Seleccione dónde guardar el archivo. Se exportarán todos los datos de estudio almacenados.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        LargePrimaryButton(
            text = if (exportStatus == ExportStatus.Exporting)
                       "Exportando datos..."
                   else
                       "Exportar datos del estudio",
            onClick = {
                if (exportStatus != ExportStatus.Exporting) {
                    exportStatus = ExportStatus.Exporting
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val filename = "aura_study_export_$ts.json"
                    onExportData(
                        filename,
                        { exportStatus = ExportStatus.Success },
                        { exportStatus = ExportStatus.Failed }
                    )
                }
            }
        )
        when (exportStatus) {
            ExportStatus.Success -> Text(
                text = "✓ Exportación completada",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            ExportStatus.Failed -> Text(
                text = "✗ No se pudo exportar el archivo",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            else -> { /* Idle or Exporting — no extra text */ }
        }

        LargeDestructiveButton("Borrar sesión actual", { pendingDelete = ResearcherDeleteAction.CurrentSession })
        LargeDestructiveButton("Borrar todos los registros", { pendingDelete = ResearcherDeleteAction.AllResearchData })
    }
}

private enum class ResearcherDeleteAction {
    CurrentSession,
    AllResearchData
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
    ExperimentTaskOrder.forEach { task ->
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
