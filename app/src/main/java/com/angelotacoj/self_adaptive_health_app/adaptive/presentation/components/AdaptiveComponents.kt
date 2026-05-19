package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ValidationType
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton
import com.angelotacoj.self_adaptive_health_app.core.ui.LargeSecondaryButton

@Composable
fun ContextualHelpBox(
    state: AdaptiveUiState,
    onHide: () -> Unit
) {
    if (!state.contextualHelpVisible || state.contextualHelpMessage.isNullOrBlank()) return
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFE8F3FF))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ayuda para continuar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF275E83))
            Text(state.contextualHelpMessage, style = MaterialTheme.typography.bodyLarge)
            LargeSecondaryButton(text = "Ocultar", onClick = onHide)
        }
    }
}

@Composable
fun AdaptiveSuggestionCard(
    pending: PendingAdaptation?,
    onApply: () -> Unit,
    onReject: () -> Unit
) {
    if (pending == null || pending.validationType != ValidationType.SUGGESTED) return
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(pending.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(pending.message, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LargeSecondaryButton(text = "Ahora no", onClick = onReject, modifier = Modifier.weight(1f))
                LargePrimaryButton(text = "Aplicar cambio", onClick = onApply, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AdaptiveConfirmationDialog(
    pending: PendingAdaptation?,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    if (pending == null || pending.validationType != ValidationType.EXPLICIT) return
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(28.dp),
        title = { Text(pending.title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(pending.message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { LargePrimaryButton(text = "Confirmar", onClick = onConfirm) },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LargeSecondaryButton(text = "Editar", onClick = onEdit)
                LargeSecondaryButton(text = "Cancelar", onClick = onCancel)
            }
        }
    )
}

@Composable
fun UndoAdaptationCard(
    visible: Boolean,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFFFF7E8))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Cambio aplicado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LargeSecondaryButton(text = "Deshacer", onClick = onUndo, modifier = Modifier.weight(1f))
                LargePrimaryButton(text = "Mantener cambio", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
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
