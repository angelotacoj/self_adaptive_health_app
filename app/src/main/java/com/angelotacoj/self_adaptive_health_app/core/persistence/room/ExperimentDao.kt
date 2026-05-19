package com.angelotacoj.self_adaptive_health_app.core.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExperimentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipantSession(entity: ParticipantSessionEntity)

    @Query("SELECT * FROM participant_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ParticipantSessionEntity?

    @Query("SELECT * FROM participant_sessions WHERE participantCode = :participantCode ORDER BY startedAt DESC")
    suspend fun getSessionByParticipantCode(participantCode: String): List<ParticipantSessionEntity>

    @Query("SELECT * FROM participant_sessions WHERE isCompleted = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): ParticipantSessionEntity?

    @Query("UPDATE participant_sessions SET endedAt = :endedAt, isCompleted = :isCompleted WHERE sessionId = :sessionId")
    suspend fun markParticipantSessionEnded(sessionId: String, endedAt: Long, isCompleted: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskRun(entity: TaskRunEntity)

    @Query("SELECT * FROM task_runs WHERE sessionId = :sessionId ORDER BY startedAt ASC")
    suspend fun getTaskRunsForSession(sessionId: String): List<TaskRunEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM task_runs " +
            "WHERE sessionId = :sessionId AND condition = :condition AND taskId = :taskId AND completed = 1)"
    )
    suspend fun isTaskCompleted(sessionId: String, condition: String, taskId: String): Boolean

    @Query(
        "UPDATE task_runs SET completed = 1, endedAt = :endedAt " +
            "WHERE sessionId = :sessionId AND condition = :condition AND taskId = :taskId"
    )
    suspend fun markTaskCompleted(sessionId: String, condition: String, taskId: String, endedAt: Long)

    @Query("SELECT COUNT(*) FROM task_runs WHERE sessionId = :sessionId AND condition = :condition AND completed = 1")
    suspend fun getCompletedTaskCount(sessionId: String, condition: String): Int

    @Query("SELECT COUNT(*) FROM task_runs WHERE sessionId = :sessionId AND completed = 1")
    suspend fun getTotalCompletedTaskCount(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionEvent(entity: InteractionEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdaptationEvent(entity: AdaptationEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserDecisionEvent(entity: UserDecisionEventEntity)

    @Query("SELECT * FROM interaction_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentInteractionEvents(limit: Int = 200): List<InteractionEventEntity>

    @Query("SELECT * FROM interaction_events WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentInteractionEventsForSession(sessionId: String, limit: Int = 200): List<InteractionEventEntity>

    @Query("SELECT * FROM adaptation_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentAdaptationEvents(limit: Int = 200): List<AdaptationEventEntity>

    @Query("SELECT * FROM adaptation_events WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentAdaptationEventsForSession(sessionId: String, limit: Int = 200): List<AdaptationEventEntity>

    @Query("SELECT * FROM user_decision_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentUserDecisionEvents(limit: Int = 200): List<UserDecisionEventEntity>

    @Query("SELECT * FROM user_decision_events WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentUserDecisionEventsForSession(sessionId: String, limit: Int = 200): List<UserDecisionEventEntity>

    @Query("SELECT COUNT(*) FROM participant_sessions")
    suspend fun participantSessionCount(): Int

    @Query("SELECT COUNT(*) FROM task_runs")
    suspend fun taskRunCount(): Int

    @Query("SELECT COUNT(*) FROM interaction_events")
    suspend fun interactionEventCount(): Int

    @Query("SELECT COUNT(*) FROM adaptation_events")
    suspend fun adaptationEventCount(): Int

    @Query("SELECT COUNT(*) FROM user_decision_events")
    suspend fun userDecisionEventCount(): Int

    @Query("SELECT COUNT(*) FROM task_runs WHERE sessionId = :sessionId")
    suspend fun taskRunCountForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM interaction_events WHERE sessionId = :sessionId")
    suspend fun interactionEventCountForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM adaptation_events WHERE sessionId = :sessionId")
    suspend fun adaptationEventCountForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM user_decision_events WHERE sessionId = :sessionId")
    suspend fun userDecisionEventCountForSession(sessionId: String): Int

    @Query("DELETE FROM participant_sessions WHERE sessionId = :sessionId")
    suspend fun deleteParticipantSession(sessionId: String)

    @Query("DELETE FROM task_runs WHERE sessionId = :sessionId")
    suspend fun deleteTaskRunsForSession(sessionId: String)

    @Query("DELETE FROM interaction_events WHERE sessionId = :sessionId")
    suspend fun deleteInteractionEventsForSession(sessionId: String)

    @Query("DELETE FROM adaptation_events WHERE sessionId = :sessionId")
    suspend fun deleteAdaptationEventsForSession(sessionId: String)

    @Query("DELETE FROM user_decision_events WHERE sessionId = :sessionId")
    suspend fun deleteUserDecisionEventsForSession(sessionId: String)

    suspend fun deleteSessionCascade(sessionId: String) {
        deleteUserDecisionEventsForSession(sessionId)
        deleteAdaptationEventsForSession(sessionId)
        deleteInteractionEventsForSession(sessionId)
        deleteTaskRunsForSession(sessionId)
        deleteParticipantSession(sessionId)
    }

    @Query("DELETE FROM participant_sessions")
    suspend fun deleteAllParticipantSessions()

    @Query("DELETE FROM task_runs")
    suspend fun deleteAllTaskRuns()

    @Query("DELETE FROM interaction_events")
    suspend fun clearInteractionEvents()

    @Query("DELETE FROM adaptation_events")
    suspend fun clearAdaptationEvents()

    @Query("DELETE FROM user_decision_events")
    suspend fun clearUserDecisionEvents()

    suspend fun clearAll() {
        clearUserDecisionEvents()
        clearAdaptationEvents()
        clearInteractionEvents()
        deleteAllTaskRuns()
        deleteAllParticipantSessions()
    }
}
