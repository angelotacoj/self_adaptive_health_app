package com.angelotacoj.self_adaptive_health_app.interview.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Phase C1.5 – DAO for short semi-structured interview responses.
 */
@Dao
interface InterviewDao {

    /** Insert or replace a single response (allows re-save). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(entity: InterviewResponseEntity)

    /** Insert or replace all responses in one batch. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllResponses(entities: List<InterviewResponseEntity>)

    /** Get all responses for a session (all 5 questions). */
    @Query("SELECT * FROM interview_responses WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    suspend fun getResponsesForSession(sessionId: String): List<InterviewResponseEntity>

    /** Get all responses for a participant across sessions. */
    @Query("SELECT * FROM interview_responses WHERE participantId = :participantId ORDER BY timestamp ASC")
    suspend fun getResponsesByParticipant(participantId: String): List<InterviewResponseEntity>

    /** Full table – used for Phase C2 JSON export. */
    @Query("SELECT * FROM interview_responses ORDER BY timestamp ASC")
    suspend fun getAllResponses(): List<InterviewResponseEntity>

    /** Count answered questions for a session. */
    @Query("SELECT COUNT(*) FROM interview_responses WHERE sessionId = :sessionId")
    suspend fun countAnsweredQuestions(sessionId: String): Int

    /** Delete all responses for a session (cascade delete support). */
    @Query("DELETE FROM interview_responses WHERE sessionId = :sessionId")
    suspend fun deleteResponsesForSession(sessionId: String)

    /** Delete all interview data (researcher panel wipe). */
    @Query("DELETE FROM interview_responses")
    suspend fun deleteAllResponses()

    // ---- Interview Status ----

    /**
     * Persist interview status (SAVED or SKIPPED) for a session.
     * Only called after an explicit evaluator action.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterviewStatus(entity: InterviewStatusEntity)

    /**
     * Returns the persisted interview status for the session, or null if PENDING.
     * A missing row means the interview has not been finished yet.
     */
    @Query("SELECT * FROM interview_status WHERE participantId = :participantId AND sessionId = :sessionId LIMIT 1")
    suspend fun getInterviewStatus(participantId: String, sessionId: String): InterviewStatusEntity?

    /** Delete interview status for a session (cascade delete support). */
    @Query("DELETE FROM interview_status WHERE sessionId = :sessionId")
    suspend fun deleteInterviewStatusForSession(sessionId: String)

    /** Full table – used for Phase C2 JSON export. */
    @Query("SELECT * FROM interview_status ORDER BY timestamp ASC")
    suspend fun getAllInterviewStatuses(): List<InterviewStatusEntity>

    /** Delete all interview statuses (researcher panel wipe). */
    @Query("DELETE FROM interview_status")
    suspend fun deleteAllInterviewStatuses()
}

