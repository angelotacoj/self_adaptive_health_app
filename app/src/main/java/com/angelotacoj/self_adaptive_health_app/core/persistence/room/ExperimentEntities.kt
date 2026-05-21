package com.angelotacoj.self_adaptive_health_app.core.persistence.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participant_sessions")
data class ParticipantSessionEntity(
    @PrimaryKey val sessionId: String,
    val participantCode: String,
    val group: String,
    val conditionOrder: String,
    val startedAt: Long,
    val endedAt: Long?,
    val isCompleted: Boolean
)

@Entity(tableName = "task_runs")
data class TaskRunEntity(
    @PrimaryKey val taskRunId: String,
    val sessionId: String,
    val participantCode: String,
    val condition: String,
    val taskId: String,
    val dataSet: String,
    val startedAt: Long,
    val endedAt: Long?,
    val completed: Boolean
)

@Entity(tableName = "interaction_events")
data class InteractionEventEntity(
    @PrimaryKey val eventId: String,
    val sessionId: String,
    val participantCode: String,
    val condition: String,
    val taskId: String?,
    val screenId: String?,
    val eventType: String,
    val oisCode: String?,
    val timestamp: Long,
    val message: String,
    val metadataJson: String?
)

@Entity(tableName = "adaptation_events")
data class AdaptationEventEntity(
    @PrimaryKey val adaptationEventId: String,
    val sessionId: String,
    val participantCode: String,
    val condition: String,
    val taskId: String?,
    val screenId: String?,
    val ruleId: String,
    val inferredDifficulty: String,
    val uiModifications: String,
    val validationType: String,
    val systemDecision: String,
    val userDecision: String?,
    val applied: Boolean,
    val undone: Boolean,
    val timestamp: Long
)

@Entity(tableName = "adaptation_preferences", primaryKeys = ["ruleId", "taskId", "screenId"])
data class AdaptationPreferenceEntity(
    val ruleId: String,
    val taskId: String,
    val screenId: String,
    val rejectedCount: Int,
    val lastRejectedAt: Long,
    val suppressAutomatic: Boolean
)

@Entity(tableName = "task_interaction_states", primaryKeys = ["taskId", "screenId"])
data class TaskStateEntity(
    val taskId: String,
    val screenId: String,
    val touchErrorCount: Int,
    val fieldErrorCount: Int,
    val helpRequestCount: Int,
    val backCount: Int,
    val screenEnteredAt: Long,
    val lastSuccessfulActionAt: Long,
    val confirmationPause: Boolean
)

@Entity(tableName = "user_decision_events")
data class UserDecisionEventEntity(
    @PrimaryKey val decisionId: String,
    val adaptationEventId: String?,
    val sessionId: String,
    val participantCode: String,
    val taskId: String?,
    val screenId: String?,
    val decision: String,
    val timestamp: Long
)

@Entity(tableName = "initial_user_profiles")
data class InitialUserProfileEntity(
    @PrimaryKey val sessionId: String,
    val participantCode: String,
    val prefersLargeText: String,
    val prefersLargeButtons: String,
    val prefersIconLabels: String,
    val prefersGuidedSteps: String,
    val prefersConfirmations: String,
    val mobileComfortLevel: String,
    val prefersErrorExamples: String,
    val prefersAdaptationPrompt: String,
    val timestamp: Long
)
