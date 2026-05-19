package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession
import com.angelotacoj.self_adaptive_health_app.core.ui.HeroHeaderCard
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer
import com.angelotacoj.self_adaptive_health_app.core.ui.SimulatedDataNoticeCard

@Composable
fun ExperimentSetupScreen(
    state: ExperimentSetupState,
    existingSessionMessage: String?,
    onAction: (ExperimentSetupAction) -> ExperimentSetupEvent?,
    onStartSession: (ExperimentSession) -> Unit,
    onContinueExistingSession: () -> Unit,
    onStartNewSession: () -> Unit,
    onDeleteExistingSession: () -> Unit,
    onOpenResearcherPanel: () -> Unit
) {
    ScreenContainer(
        title = "Configuración AURA",
        subtitle = "Preparación de la sesión experimental",
        showNotice = false
    ) {
        HeroHeaderCard(
            appName = "AURA",
            description = "Prototipo de salud móvil simulada para evaluación de interfaces."
        )

        //SimulatedDataNoticeCard()

        InstructionCard(
            title = "Antes de empezar",
            instructions = listOf(
                "Ingrese el código asignado por el investigador.",
                "Seleccione el grupo experimental para cargar los datos ficticios correctos.",
                "No ingrese información real de salud."
            )
        )

        OutlinedTextField(
            value = state.participantCode,
            onValueChange = { onAction(ExperimentSetupAction.ParticipantCodeChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Código de participante") },
            supportingText = { Text("Ejemplo: P01 o 72891968") },
            textStyle = MaterialTheme.typography.titleLarge,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Text(
            text = "Seleccione el grupo experimental",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExperimentGroup.entries.forEach { group ->
                val selected = state.selectedGroup == group
                ElevatedCard(
                    onClick = { onAction(ExperimentSetupAction.GroupSelected(group)) },
                    modifier = Modifier.fillMaxWidth(),
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = group.orderDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

/*        Text(
            text = "Información adicional",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        InstructionCard(
            title = "Orden asignado",
            instructions = listOf(
                state.selectedOrder
            )
        )*/

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
            LargeDestructiveButton("Borrar sesión anterior", onDeleteExistingSession)
        } else {
            LargePrimaryButton(
                text = "Iniciar sesión experimental",
                onClick = {
                    val event = onAction(ExperimentSetupAction.StartSessionClicked)
                    if (event is ExperimentSetupEvent.StartSession) {
                        onStartSession(event.session)
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
