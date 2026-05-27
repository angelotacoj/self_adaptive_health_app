package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession
import com.angelotacoj.self_adaptive_health_app.core.security.ResearcherPinDialog
import com.angelotacoj.self_adaptive_health_app.core.ui.HeroHeaderCard
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer

@Composable
fun ExperimentSetupScreen(
    state: ExperimentSetupState,
    generatedParticipantCode: String?,
    existingSessionMessage: String?,
    onAction: (ExperimentSetupAction) -> ExperimentSetupEvent?,
    onStartSession: (ExperimentSession) -> Unit,
    onContinueExistingSession: () -> Unit,
    onStartNewSession: () -> Unit,
    onDeleteExistingSession: () -> Unit,
    onOpenResearcherPanel: () -> Unit
) {
    var showDeletePinDialog by remember { mutableStateOf(false) }
    if (showDeletePinDialog) {
        ResearcherPinDialog(
            onConfirm = {
                showDeletePinDialog = false
                onDeleteExistingSession()
            },
            onCancel = { showDeletePinDialog = false }
        )
    }
    ScreenContainer(
        title = "Configuración inicial AURA",
        subtitle = "Preparación de la sesión experimental",
        showNotice = false
    ) {
        HeroHeaderCard(
            appName = "AURA",
            description = "Prototipo de salud móvil simulada para evaluación de interfaces."
        )

        OutlinedTextField(
            value = state.participantSuffix,
            onValueChange = { onAction(ExperimentSetupAction.ParticipantSuffixChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("participant_suffix_input"),
            label = { Text(text = "Código asignado", fontSize = 14.sp) },
            textStyle = MaterialTheme.typography.titleLarge,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (generatedParticipantCode != null && state.participantSuffix.length == 4) {
            Text(
                text = "Código generado: $generatedParticipantCode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Seleccione el grupo experimental",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExperimentGroup.entries.forEach { group ->
                val selected = state.selectedGroup == group
                ElevatedCard(
                    onClick = { onAction(ExperimentSetupAction.GroupSelected(group)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            when (group) {
                                ExperimentGroup.GroupA -> "group_a_option"
                                ExperimentGroup.GroupB -> "group_b_option"
                            }
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = group.orderDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (existingSessionMessage != null) {
            InstructionCard(
                title = "Sesión existente",
                instructions = listOf(existingSessionMessage)
            )
            LargePrimaryButton("Continuar sesión existente", onContinueExistingSession)
            LargeSecondaryButton("Iniciar nueva sesión", onStartNewSession)
            LargeDestructiveButton("Borrar sesión anterior", { showDeletePinDialog = true })
        } else {
            LargePrimaryButton(
                text = "Iniciar sesión experimental",
                modifier = Modifier.testTag("continue_button"),
                onClick = {
                    val event = onAction(ExperimentSetupAction.StartSessionClicked)
                    if (event is ExperimentSetupEvent.StartSession) {
                        val code = generatedParticipantCode
                        if (code != null) {
                            onStartSession(ExperimentSession(participantId = code, group = event.group))
                        }
                    }
                }
            )

            LargeSecondaryButton(
                text = "Panel del investigador",
                onClick = onOpenResearcherPanel
            )
        }
    }
}