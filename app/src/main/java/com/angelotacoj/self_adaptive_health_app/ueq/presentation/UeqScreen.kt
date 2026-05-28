package com.angelotacoj.self_adaptive_health_app.ueq.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
//  Design tokens – deliberately large to support older adults (60+)
// ---------------------------------------------------------------------------
private val BgColor        = Color(0xFFF4F7F5)
private val CardColor      = Color(0xFFFFFFFF)
private val AccentTeal     = Color(0xFF1B6A5F)
private val SelectedColor  = Color(0xFF1B6A5F)
private val UnselectedBg   = Color(0xFFF0F5F4)
private val SelectedTextC  = Color(0xFFFFFFFF)
private val UnselectedTextC= Color(0xFF2D4A47)
private val ErrorColor     = Color(0xFFBA3B3B)
private val ErrorBg        = Color(0xFFFFF0F0)

private val ItemFontSize       = 20.sp
private val LabelFontSize      = 16.sp
private val ScaleButtonSize    = 42.dp   // reduced from 56dp to fit 7 items in a row
private val ScaleButtonFontSz  = 16.sp
private val ButtonHeight       = 60.dp

// ---------------------------------------------------------------------------
//  Screen entry point
// ---------------------------------------------------------------------------
@Composable
fun UeqScreen(
    state: UeqScreenState,
    onEvent: (UeqEvent) -> Unit
) {
    if (state.isSaved) {
        UeqSavedConfirmation()
        return
    }
    
    if (state.alreadyCompleted) {
        Scaffold(containerColor = BgColor) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                com.angelotacoj.self_adaptive_health_app.core.ui.ScreenContainer(
                    title = "Cuestionario completado",
                    subtitle = "El cuestionario de esta condición ya fue completado.",
                    showNotice = false
                ) {
                    com.angelotacoj.self_adaptive_health_app.core.ui.LargePrimaryButton(
                        text = "Continuar al siguiente paso",
                        onClick = { onEvent(UeqEvent.AcknowledgeAlreadyCompleted) }
                    )
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Header ---
            UeqHeader(state)

            // --- Progress bar ---
            UeqProgressBar(
                current = state.currentPage + 1,
                total = state.totalItems,
                answered = state.answeredCount
            )

            // --- Animated item card ---
            val scope = rememberCoroutineScope()
            AnimatedContent(
                targetState = state.currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "ueq_page"
            ) { page ->
                val item = state.items.getOrNull(page)
                if (item != null) {
                    UeqItemCard(
                        itemNumber = item.number,
                        totalItems = state.totalItems,
                        leftLabel = item.leftLabel,
                        rightLabel = item.rightLabel,
                        selectedValue = state.selections[item.id],
                        onSelect = { value -> 
                            onEvent(UeqEvent.SelectValue(item.id, value))
                            if (!state.isLastPage) {
                                scope.launch {
                                    kotlinx.coroutines.delay(400)
                                    onEvent(UeqEvent.NextPage)
                                }
                            }
                        }
                    )
                }
            }

            // --- Validation error ---
            if (state.validationError != null) {
                UeqErrorBanner(message = state.validationError)
            }

            // --- Navigation buttons ---
            UeqNavigationRow(state = state, onEvent = onEvent)

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
//  Header
// ---------------------------------------------------------------------------
@Composable
private fun UeqHeader(state: UeqScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "UEQ",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = AccentTeal,
            fontSize = 24.sp
        )
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFE8F3F1))
        ) {
            Text(
                text = "Por favor dé su evaluación actual del producto.\nMarque sólo una opción por línea.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF2D4A47),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
        /*// Answered counter
        Text(
            text = "Respondidos: ${state.answeredCount} de ${state.totalItems}",
            style = MaterialTheme.typography.labelLarge,
            color = if (state.answeredCount == state.totalItems) AccentTeal else Color(0xFF6D8C89),
            fontSize = 15.sp
        )*/
    }
}

// ---------------------------------------------------------------------------
//  Progress bar
// ---------------------------------------------------------------------------
@Composable
private fun UeqProgressBar(current: Int, total: Int, answered: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ítem $current de $total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentTeal,
                fontSize = 17.sp
            )
            /*Text(
                text = "${(answered.toFloat() / total * 100).toInt()}% completado",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6D8C89),
                fontSize = 14.sp
            )*/
        }
        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(100.dp)),
            color = AccentTeal,
            trackColor = Color(0xFFD4E5E2)
        )
    }
}

// ---------------------------------------------------------------------------
//  Item card – semantic differential scale
// ---------------------------------------------------------------------------
@Composable
private fun UeqItemCard(
    itemNumber: Int,
    totalItems: Int,
    leftLabel: String,
    rightLabel: String,
    selectedValue: Int?,
    onSelect: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (v in 1..7) {
                    ScaleButton(
                        value = v,
                        selected = selectedValue == v,
                        onClick = { onSelect(v) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = leftLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D4A47),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D4A47),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Individual scale button  (1..7)
// ---------------------------------------------------------------------------
@Composable
private fun ScaleButton(value: Int, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) SelectedColor else UnselectedBg
    val textColor = if (selected) SelectedTextC else UnselectedTextC
    val borderColor = if (selected) SelectedColor else Color(0xFFB8D0CC)

    Box(
        modifier = Modifier
            .size(ScaleButtonSize)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (selected) 0.dp else 1.5.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$value",
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = ScaleButtonFontSz,
            color = textColor
        )
    }
}

// ---------------------------------------------------------------------------
//  Error banner
// ---------------------------------------------------------------------------
@Composable
private fun UeqErrorBanner(message: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = ErrorBg)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Advertencia",
                tint = ErrorColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = ErrorColor,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  Navigation row (Previous / Next / Save)
// ---------------------------------------------------------------------------
@Composable
private fun UeqNavigationRow(state: UeqScreenState, onEvent: (UeqEvent) -> Unit) {
    if (state.isLastPage) {
        // On the last item show a big "Guardar respuestas" button
        UeqSaveButton(
            allAnswered = state.allAnswered,
            isSaving = state.isSaving,
            onSave = { onEvent(UeqEvent.Submit) }
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when(state.currentPage){
                0 -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AccentTeal)
                            .clickable { onEvent(UeqEvent.NextPage) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Siguiente",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
                else -> {
                    TextButton(
                        onClick = { onEvent(UeqEvent.PreviousPage) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonHeight)
                    ) {
                        Text(
                            text = "Anterior",
                            fontSize = 16.sp,
                            color = AccentTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AccentTeal)
                            .clickable { onEvent(UeqEvent.NextPage) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Siguiente",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    }

    // "Ir al ítem anterior" text link when on last page but not first
    if (state.isLastPage && state.currentPage > 0) {
        TextButton(
            onClick = { onEvent(UeqEvent.PreviousPage) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Volver al ítem anterior",
                color = AccentTeal,
                fontSize = 15.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  Save button (last page)
// ---------------------------------------------------------------------------
@Composable
private fun UeqSaveButton(allAnswered: Boolean, isSaving: Boolean, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(if (allAnswered) AccentTeal else Color(0xFFB0C8C4))
            .clickable(enabled = !isSaving) { onSave() },
        contentAlignment = Alignment.Center
    ) {
        if (isSaving) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(
                    text = "Guardar respuestas",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Saved confirmation screen
// ---------------------------------------------------------------------------
@Composable
private fun UeqSavedConfirmation() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Guardado",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = "¡Gracias!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AccentTeal,
                fontSize = 28.sp
            )
            Text(
                text = "Sus respuestas han sido guardadas correctamente.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF2D4A47),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                lineHeight = 26.sp
            )
        }
    }
}
