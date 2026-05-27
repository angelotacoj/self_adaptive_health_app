package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId

object AdaptiveTiming {
    @Volatile
    var prolongedTimeDetectionEnabled: Boolean = true

    // Pilot values tuned for short lab sessions
    private const val THRESHOLD_SIMPLE = 12_000L
    private const val THRESHOLD_MEDIUM = 18_000L
    private const val THRESHOLD_COMPLEX = 25_000L
    
    // Explicit 12-second pause for OIS05
    private const val THRESHOLD_CONFIRMATION_PAUSE = 12_000L

    fun getThresholdForScreen(screenId: ScreenId): Long {
        return when (screenId) {
            // Simple screens
            ScreenId.ACCESS_CODE,
            ScreenId.ACCESS_PIN -> THRESHOLD_SIMPLE

            // Complex form/review screens
            ScreenId.WELL_BEING_REVIEW,
            ScreenId.REMINDER_REVIEW,
            ScreenId.SUMMARY_INTRO,
            ScreenId.SUMMARY_REVIEW -> THRESHOLD_COMPLEX

            // Medium screens (default)
            else -> THRESHOLD_MEDIUM
        }
    }

    fun getConfirmationPauseThreshold(): Long {
        return THRESHOLD_CONFIRMATION_PAUSE
    }
}
