package com.angelotacoj.self_adaptive_health_app.interview.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase C1.5 – Room entity for short interview responses.
 *
 * One row per question per session.
 * The composite key (sessionId, questionId) ensures uniqueness if the
 * evaluator re-opens and overwrites a response.
 *
 * NOTE: persistence is added in MIGRATION_3_4.
 * No destructive migration is used.
 */
@Entity(
    tableName = "interview_responses",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["participantId"])
    ]
)
data class InterviewResponseEntity(
    @PrimaryKey
    val responseId: String,          // "${sessionId}_${questionId}"
    val participantId: String,
    val sessionId: String,
    val questionId: String,          // e.g. "IQ01"
    val questionNumber: Int,
    val questionPrompt: String,
    val evaluatorNotes: String,      // free-text notes typed by evaluator
    val timestamp: Long
)
