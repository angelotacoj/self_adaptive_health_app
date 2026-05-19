package com.angelotacoj.self_adaptive_health_app.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
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
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ExperimentLogger
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugLogsScreen(
    logger: ExperimentLogger,
    onBack: () -> Unit
) {
    var logs by remember { mutableStateOf(logger.getLogs().asReversed()) }
    var roomRows by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val dao = AppContainer.database.experimentDao()
        val interactions = dao.recentInteractionEvents()
        val adaptations = dao.recentAdaptationEvents()
        roomRows = interactions.map {
            "${formatTime(it.timestamp)} - ${it.eventType} - ${it.condition} - ${it.taskId ?: "None"} - ${it.screenId ?: "None"} - ${it.oisCode ?: ""} - ${it.message}"
        } + adaptations.map {
            "${formatTime(it.timestamp)} - ${it.ruleId} - ${it.condition} - ${it.taskId ?: "None"} - ${it.screenId ?: "None"} - ${it.userDecision ?: it.systemDecision}"
        }
    }

    ScreenContainer(
        title = "Debug logs",
        subtitle = "Researcher-only view for in-memory MVP 1.1 interaction logs.",
        showNotice = false,
        navigationLabel = "Back",
        onNavigationClick = onBack
    ) {
        LargeDestructiveButton(
            text = "Clear logs",
            onClick = {
                logger.clear()
                logs = emptyList()
                scope.launch {
                    val dao = AppContainer.database.experimentDao()
                    dao.clearInteractionEvents()
                    dao.clearAdaptationEvents()
                    dao.clearUserDecisionEvents()
                    roomRows = emptyList()
                }
            }
        )
        LargeSecondaryButton(text = "Back to Home", onClick = onBack)

        if (logs.isEmpty() && roomRows.isEmpty()) {
            Text(text = "No logs recorded yet.")
        } else {
            roomRows.forEach { row ->
                Text(text = row)
            }
            logs.forEach { entry ->
                LogCard(entry = entry)
            }
        }
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
                text = "${formatTime(entry.timestamp)} - ${entry.eventType}",
                fontWeight = FontWeight.Bold
            )
            Text(text = "Task: ${entry.taskId?.label ?: "None"}")
            Text(text = "Screen: ${entry.screenId ?: "None"}")
            Text(text = entry.message)
            if (entry.metadata.isNotEmpty()) {
                Text(text = "Metadata: ${entry.metadata.entries.joinToString { "${it.key}=${it.value}" }}")
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))
}
