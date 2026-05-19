package com.angelotacoj.self_adaptive_health_app.navigation

sealed class AppRoute(val route: String) {
    data object ExperimentSetup : AppRoute("experiment_setup")
    data object Home : AppRoute("home")
    data object Access : AppRoute("access")
    data object Appointments : AppRoute("appointments")
    data object WellBeing : AppRoute("well_being")
    data object Reminders : AppRoute("reminders")
    data object Summary : AppRoute("summary")
    data object DebugLogs : AppRoute("debug_logs")
    data object ConditionTransition : AppRoute("condition_transition")
    data object SessionCompleted : AppRoute("session_completed")
}
