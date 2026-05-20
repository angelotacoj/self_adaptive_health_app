package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

object AdaptiveTiming {
    @Volatile
    var prolongedTimeDetectionEnabled: Boolean = true

    // Adjusted thresholds for maximum proactive adaptation
    const val THRESHOLD_SHORT = 15_000L // 15 seconds
    const val THRESHOLD_MEDIUM = 30_000L // 30 seconds
    const val THRESHOLD_LONG = 45_000L // 45 seconds
}
