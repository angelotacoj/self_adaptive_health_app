package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AppliedAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation

data class AdaptiveUiState(
    val textScale: Float = 1.0f,
    val highContrast: Boolean = false,
    val enlargedTouchTargets: Boolean = false,
    val increasedSpacing: Boolean = false,
    val showIconLabels: Boolean = true,
    val contextualHelpVisible: Boolean = false,
    val contextualHelpMessage: String? = null,
    val guidedModeEnabled: Boolean = false,
    val guidedStepMessage: String? = null,
    val reinforcedConfirmationVisible: Boolean = false,
    val safeExitEnabled: Boolean = true,
    val pendingAdaptation: PendingAdaptation? = null,
    val lastAppliedAdaptation: AppliedAdaptation? = null,
    val undoMessageVisible: Boolean = false
)
