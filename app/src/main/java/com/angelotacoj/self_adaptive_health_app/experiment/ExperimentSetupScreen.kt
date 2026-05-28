package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSession
import com.angelotacoj.self_adaptive_health_app.core.security.ResearcherPinDialog
import com.angelotacoj.self_adaptive_health_app.core.ui.HeroHeaderCard
import com.angelotacoj.self_adaptive_health_app.core.ui.InstructionCard
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeDestructiveButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer

/**
 * Phase C1.5: Group selection has been removed.
 *
 * All participants follow the fixed order: STATIC_UI → SELF_ADAPTIVE_UI.
 * [ExperimentGroup.GroupA] is stored silently as a legacy export field.
 */
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

        // Flow description (replaces the removed group selector)
        InstructionCard(
            title = "Flujo experimental",
            instructions = listOf(
                "Todos los participantes siguen el mismo orden fijo:",
                "1. Interfaz estática (STATIC) → Cuestionario UEQ",
                "2. Interfaz autoadaptativa (ADAPTIVE) → Cuestionario UEQ → Entrevista breve"
            )
        )

        OutlinedTextField(
            value = state.participantSuffix,
            onValueChange = { onAction(ExperimentSetupAction.ParticipantSuffixChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("participant_suffix_input"),
            label = { Text(text = "Código asignado al participante", fontSize = 13.sp) },
            textStyle = MaterialTheme.typography.titleSmall,
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

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