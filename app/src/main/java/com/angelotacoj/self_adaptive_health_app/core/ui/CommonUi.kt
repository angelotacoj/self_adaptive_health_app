package com.angelotacoj.self_adaptive_health_app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.LocalAdaptiveEvent
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components.LocalCurrentScreenId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import androidx.compose.ui.unit.sp
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState

private val AppBackground = Color(0xFFF5F8F6)
private val CardWhite = Color(0xFFFFFFFF)
private val SoftInfo = Color(0xFFEAF5F3)
private val SoftWarning = Color(0xFFFFF5E5)
private val SoftRed = Color(0xFFBA4A4A)
private val HeroGradient = Brush.linearGradient(
    listOf(Color(0xFFDDF3EE), Color(0xFFEAF2FF), Color(0xFFFFF7E8))
)

object StaticUiDefaults {
    val ScreenPadding = 16.dp
    val ContentSpacing = 12.dp
    val CardPadding = 14.dp
    val CardRadius = 12.dp
    val ButtonMinHeight = 48.dp
    val ButtonRadius = 12.dp
    val ButtonVerticalPadding = 10.dp
    val TitleSize = 18.sp
    val BodySize = 14.sp
    val LabelSize = 13.sp
}

object AdaptiveUiDefaults {
    val ScreenPadding = 20.dp
    val ContentSpacing = 18.dp
    val IncreasedSpacing = 24.dp
    val CardPadding = 22.dp
    val CardRadius = 22.dp
    val ButtonMinHeight = 58.dp
    val EnlargedButtonMinHeight = 68.dp
    val ButtonRadius = 20.dp
    val ButtonVerticalPadding = 16.dp
    val EnlargedButtonVerticalPadding = 20.dp
    val TitleSize = 21.sp
    val BodySize = 17.sp
    val LabelSize = 17.sp
}

private fun AdaptiveUiState.contentSpacing(): Dp = when {
    isAdaptiveMode && increasedSpacing -> AdaptiveUiDefaults.IncreasedSpacing
    isAdaptiveMode -> AdaptiveUiDefaults.ContentSpacing
    else -> StaticUiDefaults.ContentSpacing
}

private fun AdaptiveUiState.cardPadding(): Dp =
    if (isAdaptiveMode) AdaptiveUiDefaults.CardPadding else StaticUiDefaults.CardPadding

private fun AdaptiveUiState.cardRadius(): Dp =
    if (isAdaptiveMode) AdaptiveUiDefaults.CardRadius else StaticUiDefaults.CardRadius

private fun AdaptiveUiState.buttonMinHeight(): Dp = when {
    isAdaptiveMode && enlargedTouchTargets -> AdaptiveUiDefaults.EnlargedButtonMinHeight
    isAdaptiveMode -> AdaptiveUiDefaults.ButtonMinHeight
    else -> StaticUiDefaults.ButtonMinHeight
}

private fun AdaptiveUiState.buttonVerticalPadding(): Dp = when {
    isAdaptiveMode && enlargedTouchTargets -> AdaptiveUiDefaults.EnlargedButtonVerticalPadding
    isAdaptiveMode -> AdaptiveUiDefaults.ButtonVerticalPadding
    else -> StaticUiDefaults.ButtonVerticalPadding
}

private fun scaled(size: TextUnit, state: AdaptiveUiState): TextUnit =
    (size.value * if (state.isAdaptiveMode) state.textScale else 1f).sp

@Composable
fun AppScaffold(
    title: String,
    subtitle: String? = null,
    showNotice: Boolean = true,
    navigationLabel: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState(),
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ScreenContainer(
        title = title,
        subtitle = subtitle,
        showNotice = showNotice,
        navigationLabel = navigationLabel,
        onNavigationClick = onNavigationClick,
        adaptiveUiState = adaptiveUiState,
        bottomBar = bottomBar,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContainer(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showNotice: Boolean = true,
    navigationLabel: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState(),
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = adaptiveUiState.contentSpacing()
    val horizontalPadding = if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.ScreenPadding else StaticUiDefaults.ScreenPadding
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            HealthTopBar(
                title = title,
                subtitle = subtitle,
                navigationLabel = navigationLabel,
                onNavigationClick = onNavigationClick,
                adaptiveUiState = adaptiveUiState
            )
        },
        bottomBar = {
            if (bottomBar != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    bottomBar()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = if (adaptiveUiState.isAdaptiveMode) 18.dp else 12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            content()
            if (showNotice) SimulatedDataNoticeCard(adaptiveUiState = adaptiveUiState)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTopBar(
    title: String,
    subtitle: String? = null,
    navigationLabel: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 22.sp else 18.sp, adaptiveUiState)),
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = scaled(13.sp, adaptiveUiState)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onNavigationClick != null) {
                TextButton(onClick = onNavigationClick) {
                    Text("‹", style = MaterialTheme.typography.titleMedium)
                    if (adaptiveUiState.showIconLabels && navigationLabel != null) {
                        Text(" $navigationLabel", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun HeroHeaderCard(
    appName: String,
    description: String,
    modifier: Modifier = Modifier,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (adaptiveUiState.isAdaptiveMode) 28.dp else 12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(HeroGradient)
                .padding(if (adaptiveUiState.isAdaptiveMode) 24.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (adaptiveUiState.isAdaptiveMode) 12.dp else 6.dp)
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 27.sp else 20.sp, adaptiveUiState)),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF123F3A)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 17.sp else 14.sp, adaptiveUiState)),
                color = Color(0xFF33514D)
            )
        }
    }
}

@Composable
fun SessionInfoCard(
    participantId: String,
    group: String,
    condition: String,
    dataSet: String,
    modifier: Modifier = Modifier
) {
    SummaryReviewCard(
        title = "Datos de la sesión",
        rows = listOf(
            "Participante" to participantId,
            "Grupo" to group,
            "Condición" to condition,
        ),
        modifier = modifier
    )
}

@Composable
fun SafetyNotice(modifier: Modifier = Modifier) {
    SimulatedDataNoticeCard(modifier = modifier)
}

@Composable
fun SimulatedDataNoticeCard(
    modifier: Modifier = Modifier,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(adaptiveUiState.cardRadius()),
        colors = CardDefaults.outlinedCardColors(containerColor = if (adaptiveUiState.isAdaptiveMode) SoftInfo else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(adaptiveUiState.cardPadding()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Datos simulados",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 18.sp else 14.sp, adaptiveUiState)),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF245B55)
            )
            Text(
                text = "Los datos son simulados. No se almacena información clínica real.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 17.sp else 13.sp, adaptiveUiState)),
                color = Color(0xFF345B57)
            )
        }
    }
}

@Composable
fun LargePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    val onEvent = LocalAdaptiveEvent.current
    val screenId = LocalCurrentScreenId.current
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = adaptiveUiState.buttonMinHeight())
            .pointerInput(enabled) {
                if (!enabled) {
                    detectTapGestures(onTap = { onEvent(AdaptiveInteractionEventType.TOUCH_ERROR, screenId, null) })
                }
            },
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = if (adaptiveUiState.isAdaptiveMode) 22.dp else 16.dp, vertical = adaptiveUiState.buttonVerticalPadding()),
        shape = RoundedCornerShape(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.ButtonRadius else StaticUiDefaults.ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 17.sp else 14.sp, adaptiveUiState)))
    }
}

@Composable
fun LargeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    val onEvent = LocalAdaptiveEvent.current
    val screenId = LocalCurrentScreenId.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = adaptiveUiState.buttonMinHeight())
            .pointerInput(enabled) {
                if (!enabled) {
                    detectTapGestures(onTap = { onEvent(AdaptiveInteractionEventType.TOUCH_ERROR, screenId, null) })
                }
            },
        contentPadding = PaddingValues(horizontal = if (adaptiveUiState.isAdaptiveMode) 22.dp else 16.dp, vertical = adaptiveUiState.buttonVerticalPadding()),
        shape = RoundedCornerShape(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.ButtonRadius else StaticUiDefaults.ButtonRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 17.sp else 14.sp, adaptiveUiState)))
    }
}

@Composable
fun LargeDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    val onEvent = LocalAdaptiveEvent.current
    val screenId = LocalCurrentScreenId.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = adaptiveUiState.buttonMinHeight())
            .pointerInput(enabled) {
                if (!enabled) {
                    detectTapGestures(onTap = { onEvent(AdaptiveInteractionEventType.TOUCH_ERROR, screenId, null) })
                }
            },
        contentPadding = PaddingValues(horizontal = if (adaptiveUiState.isAdaptiveMode) 22.dp else 16.dp, vertical = adaptiveUiState.buttonVerticalPadding()),
        shape = RoundedCornerShape(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.ButtonRadius else StaticUiDefaults.ButtonRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 17.sp else 14.sp, adaptiveUiState)))
    }
}

@Composable
fun InfoCard(
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(adaptiveUiState.cardRadius()),
        colors = CardDefaults.elevatedCardColors(containerColor = CardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(adaptiveUiState.cardPadding()),
            verticalArrangement = Arrangement.spacedBy(if (adaptiveUiState.isAdaptiveMode && adaptiveUiState.increasedSpacing) 14.dp else 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.TitleSize else StaticUiDefaults.TitleSize, adaptiveUiState)),
                fontWeight = FontWeight.Bold
            )
            lines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.BodySize else StaticUiDefaults.BodySize, adaptiveUiState)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InstructionCard(
    title: String,
    instructions: List<String>,
    modifier: Modifier = Modifier,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    InfoCard(title = title, lines = instructions, modifier = modifier, adaptiveUiState = adaptiveUiState)
}

@Composable
fun TaskProgressHeader(
    stepText: String,
    title: String,
    modifier: Modifier = Modifier,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    val progress = parseProgress(stepText)
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(adaptiveUiState.cardRadius()),
        colors = CardDefaults.outlinedCardColors(containerColor = if (adaptiveUiState.guidedModeEnabled) SoftWarning else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(adaptiveUiState.cardPadding()), verticalArrangement = Arrangement.spacedBy(if (adaptiveUiState.isAdaptiveMode) 10.dp else 4.dp)) {
            Text(stepText, style = MaterialTheme.typography.labelLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) 16.sp else 12.sp, adaptiveUiState)), color = Color(0xFF6F541C), fontWeight = FontWeight.Bold)
            if (adaptiveUiState.guidedModeEnabled) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFFE8DCC3)
                )
            }
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.TitleSize else StaticUiDefaults.TitleSize, adaptiveUiState)), fontWeight = FontWeight.Bold)
            if (adaptiveUiState.guidedModeEnabled && adaptiveUiState.guidedStepMessage != null) {
                Text(
                    text = adaptiveUiState.guidedStepMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SummaryReviewCard(
    title: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(adaptiveUiState.cardRadius()),
        colors = CardDefaults.elevatedCardColors(containerColor = CardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(adaptiveUiState.cardPadding()),
            verticalArrangement = Arrangement.spacedBy(if (adaptiveUiState.isAdaptiveMode && adaptiveUiState.increasedSpacing) 16.dp else 8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.TitleSize else StaticUiDefaults.TitleSize, adaptiveUiState)), fontWeight = FontWeight.Bold)
            rows.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.LabelSize else StaticUiDefaults.LabelSize, adaptiveUiState)), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.BodySize else StaticUiDefaults.BodySize, adaptiveUiState)), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    taskNumber: String? = null,
    status: String? = null,
    enabled: Boolean = true,
    startButtonTestTag: String? = null,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (adaptiveUiState.isAdaptiveMode) 22.dp else 12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(adaptiveUiState.cardPadding()),
            verticalArrangement = Arrangement.spacedBy(if (adaptiveUiState.isAdaptiveMode && adaptiveUiState.increasedSpacing) 16.dp else 8.dp)
        ) {
            if (taskNumber != null || status != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (taskNumber != null) Text(taskNumber, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (status != null) Text(status, color = Color(0xFF4C7B52), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.TitleSize else StaticUiDefaults.TitleSize, adaptiveUiState)), fontWeight = FontWeight.Bold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = scaled(if (adaptiveUiState.isAdaptiveMode) AdaptiveUiDefaults.BodySize else StaticUiDefaults.BodySize, adaptiveUiState)),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LargePrimaryButton(
                text = buttonText,
                onClick = onClick,
                modifier = if (startButtonTestTag != null) Modifier.testTag(startButtonTestTag) else Modifier,
                enabled = enabled,
                adaptiveUiState = adaptiveUiState
            )
        }
    }
}

@Composable
fun ButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    adaptiveUiState: AdaptiveUiState = AdaptiveUiState()
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LargeSecondaryButton(text = secondaryText, onClick = onSecondary, modifier = Modifier.weight(1f), adaptiveUiState = adaptiveUiState)
        LargePrimaryButton(text = primaryText, onClick = onPrimary, modifier = Modifier.weight(1f), adaptiveUiState = adaptiveUiState)
    }
}

private fun parseProgress(stepText: String): Float {
    val numbers = Regex("(\\d+)").findAll(stepText).map { it.value.toFloat() }.toList()
    return if (numbers.size >= 2 && numbers[1] > 0f) (numbers[0] / numbers[1]).coerceIn(0f, 1f) else 1f
}
