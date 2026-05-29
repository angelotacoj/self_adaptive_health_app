package com.angelotacoj.self_adaptive_health_app.interview.persistence

import androidx.room.Entity

/**
 * Persisted interview completion status scoped by participantId + sessionId.
 *
 * Status values:
 *  - SAVED    – evaluator explicitly saved interview responses (even if all empty after confirmation)
 *  - SKIPPED  – evaluator explicitly confirmed skip
 *
 * A row is only inserted after an intentional evaluator action.
 * If no row exists, the interview is PENDING (not yet finished).
 *
 * DB version = 1 (no migration needed; requires clean install during development).
 */
@Entity(
    tableName = "interview_status",
    primaryKeys = ["participantId", "sessionId"]
)
data class InterviewStatusEntity(
    val participantId: String,
    val sessionId: String,
    /** "SAVED" or "SKIPPED" */
    val status: String,
    val timestamp: Long
)
