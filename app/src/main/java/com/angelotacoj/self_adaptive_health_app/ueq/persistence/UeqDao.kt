package com.angelotacoj.self_adaptive_health_app.ueq.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UeqDao {

    /** Insert a single item response. Replace on conflict (re-submit). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUeqResponse(entity: UeqResponseEntity)

    /** Insert all 26 item responses in one transaction. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUeqResponses(entities: List<UeqResponseEntity>)

    /** Retrieve all 26 responses for a specific condition in a session. */
    @Query("SELECT * FROM ueq_responses WHERE sessionId = :sessionId AND condition = :condition ORDER BY itemNumber ASC")
    suspend fun getResponsesForCondition(sessionId: String, condition: String): List<UeqResponseEntity>

    /** Retrieve all responses for a participant across all sessions and conditions. */
    @Query("SELECT * FROM ueq_responses WHERE participantId = :participantId ORDER BY timestamp ASC, itemNumber ASC")
    suspend fun getResponsesByParticipant(participantId: String): List<UeqResponseEntity>

    /** Retrieve all responses for a session (both conditions). */
    @Query("SELECT * FROM ueq_responses WHERE sessionId = :sessionId ORDER BY condition ASC, itemNumber ASC")
    suspend fun getResponsesBySession(sessionId: String): List<UeqResponseEntity>

    /** Retrieve all responses for a specific condition (across all sessions, for aggregate analysis). */
    @Query("SELECT * FROM ueq_responses WHERE condition = :condition ORDER BY sessionId ASC, itemNumber ASC")
    suspend fun getResponsesByCondition(condition: String): List<UeqResponseEntity>

    /** Retrieve all UEQ responses (for JSON export in Phase C2). */
    @Query("SELECT * FROM ueq_responses ORDER BY sessionId ASC, condition ASC, itemNumber ASC")
    suspend fun getAllUeqResponses(): List<UeqResponseEntity>

    /** Count how many items have been answered in a given session + condition (0..26). */
    @Query("SELECT COUNT(*) FROM ueq_responses WHERE participantId = :participantId AND sessionId = :sessionId AND condition = :condition AND questionnaireMode = :questionnaireMode")
    suspend fun countAnsweredItems(participantId: String, sessionId: String, condition: String, questionnaireMode: String): Int

    /** True if all 26 items are saved for a session + condition. */
    @Query("SELECT COUNT(*) >= 26 FROM ueq_responses WHERE participantId = :participantId AND sessionId = :sessionId AND condition = :condition AND questionnaireMode = :questionnaireMode")
    suspend fun isUeqComplete(participantId: String, sessionId: String, condition: String, questionnaireMode: String): Boolean

    /** Delete responses for a session (used in cascade delete). */
    @Query("DELETE FROM ueq_responses WHERE sessionId = :sessionId")
    suspend fun deleteResponsesForSession(sessionId: String)

    /** Delete all UEQ responses (used in full data wipe). */
    @Query("DELETE FROM ueq_responses")
    suspend fun deleteAllUeqResponses()
}
