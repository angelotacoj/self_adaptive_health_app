package com.angelotacoj.self_adaptive_health_app.adaptive.domain.model

import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId

enum class ExperimentCondition {
    STATIC_UI,
    SELF_ADAPTIVE_UI
}

enum class ObservedInteractionSignal {
    OIS01_TIME_ON_SCREEN,
    OIS02_BACKTRACKING,
    OIS03_HELP_REQUEST,
    OIS04_FIELD_ERROR,
    OIS05_CONFIRMATION_PAUSE
}

enum class InferredDifficulty {
    DI01_LEGIBILITY_DIFFICULTY,
    DI02_COMPREHENSION_ORIENTATION_DIFFICULTY,
    DI03_RECOVERY_UNCERTAINTY_DIFFICULTY,
    DI04_CONTROL_CONFIRMATION_DOUBT
}

enum class UiModification {
    UIM01_TEXT,
    UIM02_CONTEXTUAL_HELP_STEP_BY_STEP,
    UIM03_REINFORCED_CONFIRMATION,
    UIM04_VISUAL_FEEDBACK
}

enum class AdaptationRuleId {
    AR01_TIME_ON_SCREEN,
    AR02_BACKTRACKING,
    AR03_HELP_REQUEST,
    AR04_FIELD_ERROR,
    AR05_CONFIRMATION_PAUSE
}

enum class AdaptationLevel(val levelValue: Int) {
    LEVEL_0_BASE(0),
    LEVEL_1_LIGHT_SUPPORT(1),
    LEVEL_2_MODERATE_SUPPORT(2),
    LEVEL_3_HIGH_SUPPORT(3)
}

enum class UserDecision {
    ACCEPTED,
    REJECTED,
    UNDONE,
    NOT_REQUIRED
}

enum class ValidationType {
    NON_INTRUSIVE,
    DIRECT,
    SUGGESTED,
    EXPLICIT,
    NOT_APPLICABLE
}

data class ReviewSummary(
    val title: String,
    val details: Map<String, String>
)

data class PendingAdaptation(
    val ruleId: AdaptationRuleId,
    val title: String,
    val message: String,
    val modifications: List<UiModification>,
    val validationType: ValidationType,
    val reviewSummary: ReviewSummary? = null
)

data class AppliedAdaptation(
    val ruleId: AdaptationRuleId,
    val modifications: List<UiModification>,
    val previousState: Map<String, Any> = emptyMap(),
    val appliedAt: Long = System.currentTimeMillis(),
    val level: AdaptationLevel = AdaptationLevel.LEVEL_1_LIGHT_SUPPORT
)

data class AdaptationPlan(
    val adaptationEventId: String,
    val ruleId: AdaptationRuleId,
    val signals: List<ObservedInteractionSignal>,
    val difficulties: List<InferredDifficulty>,
    val modifications: List<UiModification>,
    val validationType: ValidationType,
    val title: String,
    val message: String,
    val taskId: TaskId?,
    val screenId: ScreenId?,
    val reviewSummary: ReviewSummary? = null,
    val targetLevel: AdaptationLevel = AdaptationLevel.LEVEL_1_LIGHT_SUPPORT
)

enum class ValidationResult {
    APPROVED,
    ADJUSTED,
    REJECTED
}

data class ControlledAdaptationPlan(
    val plan: AdaptationPlan,
    val canShow: Boolean,
    val result: ValidationResult = if (canShow) ValidationResult.APPROVED else ValidationResult.REJECTED,
    val reason: String? = null
)

data class AdaptiveInteractionEvent(
    val taskId: TaskId?,
    val screenId: ScreenId?,
    val eventType: AdaptiveInteractionEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val reviewSummary: ReviewSummary? = null
)

enum class AdaptiveInteractionEventType {
    TOUCH_ERROR,
    PROLONGED_TIME,
    CONFIRMATION_PAUSE,
    BACK_PRESSED,
    HELP_REQUESTED,
    FIELD_ERROR,
    ADAPTATION_REJECTED,
    SENSITIVE_ACTION
}

data class TaskInteractionState(
    val taskId: TaskId?,
    val screenId: ScreenId?,
    val screenEnteredAt: Long,
    val successfulActionAt: Long,
    val backCountInTask: Int,
    val fieldErrorCount: Int,
    val touchErrorCount: Int = 0,
    val helpRequestCount: Int = 0,
    val confirmationPause: Boolean = false
)

data class KnowledgeSnapshot(
    val rejectedRulesForTask: Set<AdaptationRuleId>,
    val suggestionsShownForTask: Int,
    val modalShownForScreen: Boolean,
    val lastRejectionAt: Long?,
    val rejectedRuleLevelsForTask: Set<Pair<AdaptationRuleId, AdaptationLevel>> = emptySet()
)
