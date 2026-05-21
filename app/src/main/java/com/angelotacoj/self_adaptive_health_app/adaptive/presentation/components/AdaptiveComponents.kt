package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.UiModification
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton

@Composable
fun ContextualHelpBox(
    state: AdaptiveUiState,
    onHide: () -> Unit
) {
    AnimatedVisibility(
        visible = state.isAdaptiveMode && state.contextualHelpVisible && !state.contextualHelpMessage.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(animationSpec = tween(500))
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = if (state.highContrast) MaterialTheme.colorScheme.surface else Color(0xFFE8F3FF))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Ayuda del sistema",
                    style = MaterialTheme.typography.titleLarge.scaled(state),
                    fontWeight = FontWeight.Bold,
                    color = if (state.highContrast) MaterialTheme.colorScheme.primary else Color(0xFF275E83)
                )
                Text(state.contextualHelpMessage ?: "", style = MaterialTheme.typography.bodyLarge.scaled(state))
                LargeSecondaryButton(text = "Entendido, ocultar", onClick = onHide, adaptiveUiState = state)
            }
        }
    }
}

@Composable
fun AdaptiveSuggestionCard(
    pending: PendingAdaptation?,
    onApply: () -> Unit,
    onReject: () -> Unit,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    AnimatedVisibility(
        visible = adaptiveUiState.isAdaptiveMode && pending != null && pending.validationType == ValidationType.SUGGESTED,
        enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(animationSpec = tween(500))
    ) {
        pending?.let {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Sugerencia de adaptación", style = MaterialTheme.typography.titleSmall.scaled(adaptiveUiState), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    Text(it.title, style = MaterialTheme.typography.titleLarge.scaled(adaptiveUiState), fontWeight = FontWeight.Bold)
                    Text(it.message, style = MaterialTheme.typography.bodyLarge.scaled(adaptiveUiState))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LargeSecondaryButton(text = "No por ahora", onClick = onReject, modifier = Modifier.weight(1f), adaptiveUiState = adaptiveUiState)
                        LargePrimaryButton(text = "Sí, aplicar", onClick = onApply, modifier = Modifier.weight(1f), adaptiveUiState = adaptiveUiState)
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveConfirmationDialog(
    pending: PendingAdaptation?,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    if (!adaptiveUiState.isAdaptiveMode || pending == null || pending.validationType != ValidationType.EXPLICIT) return
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(28.dp),
        title = { Text(pending.title, style = MaterialTheme.typography.titleLarge.scaled(adaptiveUiState)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(pending.message, style = MaterialTheme.typography.bodyLarge.scaled(adaptiveUiState))
                if (pending.reviewSummary != null) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(pending.reviewSummary.title, style = MaterialTheme.typography.titleMedium.scaled(adaptiveUiState), fontWeight = FontWeight.Bold)
                            pending.reviewSummary.details.forEach { (label, value) ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium.scaled(adaptiveUiState), color = MaterialTheme.colorScheme.primary)
                                    Text(value, style = MaterialTheme.typography.bodyLarge.scaled(adaptiveUiState))
                                }
                            }
                        }
                    }
                }
                LargePrimaryButton(text = "Confirmar y continuar", onClick = onConfirm, adaptiveUiState = adaptiveUiState)
                LargeSecondaryButton(text = "Revisar datos", onClick = onEdit, adaptiveUiState = adaptiveUiState)
                LargeSecondaryButton(text = "Cancelar", onClick = onCancel, adaptiveUiState = adaptiveUiState)
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun UndoAdaptationCard(
    visible: Boolean,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    var showDetails by remember { mutableStateOf(false) }
    val applied = adaptiveUiState.lastAppliedAdaptation
    AnimatedVisibility(
        visible = adaptiveUiState.isAdaptiveMode && visible,
        enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(animationSpec = tween(500))
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = if (adaptiveUiState.highContrast) MaterialTheme.colorScheme.surface else Color(0xFFFFF7E8))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Cambio aplicado automáticamente",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.scaled(adaptiveUiState),
                        fontWeight = FontWeight.Bold,
                        color = if (adaptiveUiState.highContrast) MaterialTheme.colorScheme.primary else Color(0xFF8B5E34)
                    )
                    IconButton(onClick = { showDetails = true }) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            if (adaptiveUiState.showIconLabels) {
                                Text("Información", style = MaterialTheme.typography.labelLarge.scaled(adaptiveUiState), modifier = Modifier.padding(end = 4.dp))
                            }
                            Text(
                                text = "i",
                                style = MaterialTheme.typography.titleLarge.scaled(adaptiveUiState),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (adaptiveUiState.highContrast) MaterialTheme.colorScheme.primary else Color(0xFF8B5E34)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LargeSecondaryButton(text = "Deshacer cambio", onClick = onUndo, modifier = Modifier.weight(1f), adaptiveUiState = adaptiveUiState)
                    LargePrimaryButton(text = "Mantener así", onClick = onDismiss, modifier = Modifier.weight(1f), adaptiveUiState = adaptiveUiState)
                }
            }
        }
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Qué cambió en la interfaz", style = MaterialTheme.typography.titleLarge.scaled(adaptiveUiState)) },
            text = {
                val modifications = applied?.modifications.orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "AURA aplicó estos ajustes para facilitar la tarea actual:",
                        style = MaterialTheme.typography.bodyLarge.scaled(adaptiveUiState)
                    )
                    if (modifications.isEmpty()) {
                        Text(
                            text = "• Se aplicó una ayuda visual para continuar con la tarea.",
                            style = MaterialTheme.typography.bodyLarge.scaled(adaptiveUiState)
                        )
                    }
                    modifications.forEach { modification ->
                        Text(
                            text = "• ${modification.description()}",
                            style = MaterialTheme.typography.bodyLarge.scaled(adaptiveUiState)
                        )
                    }
                }
            },
            confirmButton = {
                LargePrimaryButton("Entendido", { showDetails = false }, adaptiveUiState = adaptiveUiState)
            }
        )
    }
}

private fun UiModification.description(): String {
    return when (this) {
        UiModification.UIM01_TEXT_SIZE -> "Se aumentó el tamaño del texto para mejorar la lectura."
        UiModification.UIM02_CONTRAST -> "Se reforzó el contraste para distinguir mejor la información importante."
        UiModification.UIM03_TOUCH_TARGETS -> "Se ampliaron las áreas táctiles de los controles."
        UiModification.UIM04_SPACING -> "Se aumentó el espacio entre elementos para reducir toques accidentales."
        UiModification.UIM05_ICONS_LABELS -> "Se reforzaron etiquetas e indicadores visuales."
        UiModification.UIM06_CONTEXTUAL_HELP -> "Se mostró ayuda contextual relacionada con la pantalla actual."
        UiModification.UIM07_GUIDED_NAVIGATION -> "Se activó una guía paso a paso para orientar la navegación."
        UiModification.UIM08_REINFORCED_CONFIRMATION -> "Se agregó una confirmación reforzada antes de continuar."
        UiModification.UIM09_VISUAL_FEEDBACK -> "Se mostró retroalimentación visual sobre el siguiente paso o el resultado."
        UiModification.UIM10_SAFE_EXIT -> "Se mantuvieron opciones claras para volver, cancelar o corregir."
    }
}

@Composable
fun AdaptiveSnackbar(data: SnackbarData) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = if (MaterialTheme.colorScheme.primary == Color(0xFF000000)) Color(0xFF000000) else Color(0xFF123F3A)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "AURA ajustó la interfaz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (MaterialTheme.colorScheme.primary == Color(0xFF000000)) Color(0xFFFFFFFF) else Color(0xFFEAF5F3)
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFFFFF)
            )
        }
    }
}

private fun TextStyle.scaled(state: AdaptiveUiState): TextStyle {
    val currentSize = fontSize
    val currentLineHeight = lineHeight
    return copy(
        fontSize = if (currentSize == TextUnit.Unspecified) currentSize else (currentSize.value * state.textScale).sp,
        lineHeight = if (currentLineHeight == TextUnit.Unspecified) currentLineHeight else (currentLineHeight.value * state.textScale).sp
    )
}

@Composable
fun AdaptiveText(
    text: String,
    state: AdaptiveUiState,
    modifier: Modifier = Modifier,
    title: Boolean = false
) {
    val baseSize = if (title) 22 else 18
    Text(
        text = text,
        modifier = modifier,
        fontSize = (baseSize * state.textScale).sp,
        lineHeight = (baseSize * state.textScale + 8).sp,
        fontWeight = if (title) FontWeight.Bold else FontWeight.Normal
    )
}
