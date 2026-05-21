package com.angelotacoj.self_adaptive_health_app.adaptive.presentation.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptiveInteractionEventType
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId

val LocalAdaptiveEvent = staticCompositionLocalOf<(AdaptiveInteractionEventType, ScreenId, com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ReviewSummary?) -> Boolean> { { _, _, _ -> false } }
val LocalCurrentScreenId = staticCompositionLocalOf<ScreenId> { ScreenId.HOME }