package com.angelotacoj.self_adaptive_health_app.core.security

object ResearcherSecurity {
    // TODO: Replace this debug PIN with secure configuration before real deployment.
    const val RESEARCHER_PIN = "123456"

    fun isValidResearcherPin(pin: String): Boolean {
        return pin.length == 6 && pin.all { it.isDigit() } && pin == RESEARCHER_PIN
    }
}
