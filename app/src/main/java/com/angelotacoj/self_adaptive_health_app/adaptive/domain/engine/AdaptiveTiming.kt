package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

object AdaptiveTiming {
    @Volatile
    var prolongedTimeDetectionEnabled: Boolean = true

    // Tuned for short lab sessions: adaptations require user dwell time, but are observable.
    const val THRESHOLD_SHORT = 12_000L
    const val THRESHOLD_MEDIUM = 22_000L
    const val THRESHOLD_LONG = 32_000L
}
