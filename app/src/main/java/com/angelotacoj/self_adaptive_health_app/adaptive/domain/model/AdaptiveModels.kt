package com.angelotacoj.self_adaptive_health_app.adaptive.domain.model

import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId

enum class ExperimentCondition {
    STATIC_UI,
    SELF_ADAPTIVE_UI
}

enum class ObservedInteractionSignal {
    OIS01_TOUCH_ERRORS,
    OIS02_PROLONGED_TIME,
    OIS03_CONFIRMATION_PAUSE,
    OIS04_BACKTRACKING,
    OIS05_HELP_REQUEST,
    OIS06_FIELD_ERROR,
    OIS07_ADAPTATION_REJECTED,
    OIS08_SENSITIVE_ACTION
}

enum class InferredDifficulty {
    DI01_MOTOR_PRECISION,
    DI02_VISUAL_OR_COGNITIVE,
    DI03_FLOW_DISORIENTATION,
    DI04_DOUBT_OR_NEED_FOR_CONTROL,
    DI05_USER_PREFERENCE_OR_LOSS_OF_CONTROL
}

enum class UiModification {
    UIM01_TEXT_SIZE,
    UIM02_CONTRAST,
    UIM03_TOUCH_TARGETS,
    UIM04_SPACING,
    UIM05_ICONS_LABELS,
    UIM06_CONTEXTUAL_HELP,
    UIM07_GUIDED_NAVIGATION,
    UIM08_REINFORCED_CONFIRMATION,
    UIM09_VISUAL_FEEDBACK,
    UIM10_SAFE_EXIT
}

enum class AdaptationRuleId {
    AR01,
    AR02,
    AR03,
    AR04,
    AR05,
    AR06,
    AR07,
    AR08
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
    val appliedAt: Long = System.currentTimeMillis()
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
    val reviewSummary: ReviewSummary? = null
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
    val lastRejectionAt: Long?
)
