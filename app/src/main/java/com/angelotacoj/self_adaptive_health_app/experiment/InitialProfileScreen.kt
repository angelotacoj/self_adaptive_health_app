package com.angelotacoj.self_adaptive_health_app.experiment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer

@Composable
fun InitialProfileScreen(
    onProfileComplete: (
        prefersLargeText: String,
        prefersLargeButtons: String,
        prefersIconLabels: String,
        prefersGuidedSteps: String,
        prefersConfirmations: String,
        mobileComfortLevel: String,
        prefersErrorExamples: String,
        prefersAdaptationPrompt: String
    ) -> Unit
) {
    var q1 by remember { mutableStateOf("") }
    var q2 by remember { mutableStateOf("") }
    var q3 by remember { mutableStateOf("") }
    var q4 by remember { mutableStateOf("") }
    var q5 by remember { mutableStateOf("") }
    var q6 by remember { mutableStateOf("") }
    var q7 by remember { mutableStateOf("") }
    var q8 by remember { mutableStateOf("") }

    val isComplete = q1.isNotEmpty() && q2.isNotEmpty() && q3.isNotEmpty() &&
            q4.isNotEmpty() && q5.isNotEmpty() && q6.isNotEmpty() &&
            q7.isNotEmpty() && q8.isNotEmpty()

    ScreenContainer(
        title = "Perfil inicial",
        subtitle = "Por favor, responda a estas preguntas para personalizar su experiencia.",
        showNotice = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileQuestion(
                question = "¿Prefiere que el texto de la aplicación sea más grande?",
                options = listOf("Sí", "No", "No estoy seguro(a)"),
                selected = q1,
                onSelect = { q1 = it },
                optionTags = mapOf("Sí" to "profile_question_1_yes")
            )

            ProfileQuestion(
                question = "¿Le resulta más cómodo usar botones grandes y separados?",
                options = listOf("Sí", "No", "No estoy seguro(a)"),
                selected = q2,
                onSelect = { q2 = it },
                optionTags = mapOf("Sí" to "profile_question_2_yes")
            )

            ProfileQuestion(
                question = "¿Prefiere que los íconos siempre tengan texto explicativo?",
                options = listOf("Sí", "No", "No estoy seguro(a)"),
                selected = q3,
                onSelect = { q3 = it },
                optionTags = mapOf("Sí" to "profile_question_3_yes")
            )

            ProfileQuestion(
                question = "¿Le gustaría recibir instrucciones paso a paso en tareas con varios pasos?",
                options = listOf("Sí", "Solo si lo necesito", "No"),
                selected = q4,
                onSelect = { q4 = it },
                optionTags = mapOf("Solo si lo necesito" to "profile_question_4_only_if_needed")
            )

            ProfileQuestion(
                question = "¿Desea que la aplicación muestre una confirmación antes de guardar o finalizar información?",
                options = listOf("Sí", "Solo en acciones importantes", "No"),
                selected = q5,
                onSelect = { q5 = it },
                optionTags = mapOf("Solo en acciones importantes" to "profile_question_5_important_only")
            )

            ProfileQuestion(
                question = "¿Qué tan cómodo(a) se siente usando aplicaciones móviles?",
                options = listOf("Poco cómodo(a)", "Regular", "Cómodo(a)"),
                selected = q6,
                onSelect = { q6 = it },
                optionTags = mapOf("Regular" to "profile_question_6_regular")
            )

            ProfileQuestion(
                question = "Cuando aparece un error en una aplicación, ¿prefiere que se le muestre un ejemplo de cómo corregirlo?",
                options = listOf("Sí", "No", "No estoy seguro(a)"),
                selected = q7,
                onSelect = { q7 = it },
                optionTags = mapOf("Sí" to "profile_question_7_yes")
            )

            ProfileQuestion(
                question = "¿Prefiere que la aplicación le pregunte antes de cambiar la forma en que se ve la pantalla?",
                options = listOf("Sí", "Solo en cambios importantes", "No"),
                selected = q8,
                onSelect = { q8 = it },
                optionTags = mapOf("Solo en cambios importantes" to "profile_question_8_important_only")
            )

            Spacer(modifier = Modifier.height(16.dp))

            LargePrimaryButton(
                text = "Guardar y continuar",
                modifier = Modifier.testTag("profile_continue_button"),
                onClick = {
                    onProfileComplete(q1, q2, q3, q4, q5, q6, q7, q8)
                },
                enabled = isComplete
            )
        }
    }
}

@Composable
fun ProfileQuestion(
    question: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    optionTags: Map<String, String> = emptyMap()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            options.forEach { option ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        modifier = optionTags[option]?.let { Modifier.testTag(it) } ?: Modifier
                    )
                    Text(text = option, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
