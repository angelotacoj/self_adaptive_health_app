package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AppliedAdaptation
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.PendingAdaptation

data class AdaptiveUiState(
    val isAdaptiveMode: Boolean = false,
    val uim01Level: AdaptationLevel = AdaptationLevel.LEVEL_0_BASE,
    val uim02Level: AdaptationLevel = AdaptationLevel.LEVEL_0_BASE,
    val uim03Level: AdaptationLevel = AdaptationLevel.LEVEL_0_BASE,
    val uim04Level: AdaptationLevel = AdaptationLevel.LEVEL_0_BASE,
    val contextualHelpMessage: String? = null,
    val guidedStepMessage: String? = null,
    val pendingAdaptation: PendingAdaptation? = null,
    val lastAppliedAdaptation: AppliedAdaptation? = null,
    val undoMessageVisible: Boolean = false,
    val snackbarMessage: String? = null,
    val contextualHelpVisibleOverride: Boolean? = null
) {
    // UIM01: Text legibility, contrast, and layout scaling
    val textScale: Float
        get() = when (uim01Level) {
            AdaptationLevel.LEVEL_0_BASE -> 1.0f
            AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> 1.15f
            AdaptationLevel.LEVEL_2_MODERATE_SUPPORT -> 1.25f
            AdaptationLevel.LEVEL_3_HIGH_SUPPORT -> 1.35f
        }

    val highContrast: Boolean
        get() = when (uim01Level) {
            AdaptationLevel.LEVEL_0_BASE, AdaptationLevel.LEVEL_1_LIGHT_SUPPORT -> false
            AdaptationLevel.LEVEL_2_MODERATE_SUPPORT, AdaptationLevel.LEVEL_3_HIGH_SUPPORT -> true
        }

    val enlargedTouchTargets: Boolean
        get() = uim01Level == AdaptationLevel.LEVEL_3_HIGH_SUPPORT

    val increasedSpacing: Boolean
        get() = uim01Level == AdaptationLevel.LEVEL_3_HIGH_SUPPORT

    // UIM02: Guided orientation & step-by-step assistant
    val showIconLabels: Boolean
        get() = uim02Level.levelValue >= AdaptationLevel.LEVEL_1_LIGHT_SUPPORT.levelValue

    val contextualHelpVisible: Boolean
        get() = contextualHelpVisibleOverride ?: (
            uim02Level.levelValue >= AdaptationLevel.LEVEL_2_MODERATE_SUPPORT.levelValue ||
            uim04Level.levelValue >= AdaptationLevel.LEVEL_1_LIGHT_SUPPORT.levelValue
        )

    val guidedModeEnabled: Boolean
        get() = uim02Level == AdaptationLevel.LEVEL_3_HIGH_SUPPORT

    // UIM03: Confirmation doubts and verification guards
    val reinforcedConfirmationVisible: Boolean
        get() = uim03Level.levelValue >= AdaptationLevel.LEVEL_2_MODERATE_SUPPORT.levelValue

    // UIM04: Recoverability feedback and safe action exit
    val safeExitEnabled: Boolean
        get() = uim04Level == AdaptationLevel.LEVEL_3_HIGH_SUPPORT
}
