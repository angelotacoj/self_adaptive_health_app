package com.angelotacoj.self_adaptive_health_app.interview.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer

/**
 * Phase C1.5 – Short semi-structured interview screen.
 *
 * Shows all 5 questions on a single scrollable page so the evaluator can
 * type notes for each answer during the verbal interview.
 *
 * Design intent:
 *  - Evaluator-facing (not participant-facing).
 *  - Large text fields, clear labels, relaxed constraints.
 *  - Skip is intentionally allowed: documented in protocol.
 */
@Composable
fun InterviewScreen(
    state: InterviewScreenState,
    onEvent: (InterviewEvent) -> Unit
) {
    if (state.isSaved) {
        // Confirmation screen
        ScreenContainer(
            title = "Entrevista guardada",
            subtitle = "Los datos han sido registrados.",
            showNotice = false
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "¡Entrevista completada!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = "Continúe con el siguiente paso.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        return
    }

    ScreenContainer(
        title = "Entrevista breve",
        subtitle = "Registre las respuestas del participante.",
        showNotice = false
    ) {

        // Header card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Esta es una entrevista semiestructurada. Haga cada pregunta en voz alta y registre un resumen de la respuesta del participante en el campo correspondiente. Puede omitir preguntas si el participante prefiere no responder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(Modifier.height(8.dp))

        // Progress indicator
        Text(
            text = "Respuestas registradas: ${state.answeredCount} de ${state.totalQuestions}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        // One card per question
        state.questions.forEachIndexed { index, question ->
            QuestionCard(
                number = question.number,
                prompt = question.prompt,
                notes = state.notes[question.id].orEmpty(),
                onNotesChanged = { text -> onEvent(InterviewEvent.NotesChanged(question.id, text)) }
            )
            Spacer(Modifier.height(12.dp))
        }

        // Error banner
        if (state.saveError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = state.saveError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.isSaving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LargePrimaryButton(
                text = "Guardar entrevista",
                onClick = { onEvent(InterviewEvent.Save) }
            )
            Spacer(Modifier.height(8.dp))
            LargeSecondaryButton(
                text = "Omitir entrevista",
                onClick = { onEvent(InterviewEvent.Skip) }
            )
        }
    }
}

@Composable
private fun QuestionCard(
    number: Int,
    prompt: String,
    notes: String,
    onNotesChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Question number badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notas del evaluador") },
            placeholder = { Text("Escriba un resumen de la respuesta...") },
            minLines = 3,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            ),
            shape = MaterialTheme.shapes.medium
        )
    }
}
