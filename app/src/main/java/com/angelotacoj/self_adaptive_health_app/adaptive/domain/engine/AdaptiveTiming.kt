package com.angelotacoj.self_adaptive_health_app.adaptive.domain.engine

object AdaptiveTiming {
    @Volatile
    var prolongedTimeDetectionEnabled: Boolean = true
}
