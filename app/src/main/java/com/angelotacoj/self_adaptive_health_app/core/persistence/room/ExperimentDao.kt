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

    @Query("SELECT * FROM participant_sessions WHERE participantId = :participantId ORDER BY startedAt DESC")
    suspend fun getSessionByParticipantCode(participantId: String): List<ParticipantSessionEntity>

    @Query(
        "SELECT MAX(CAST(SUBSTR(participantId, 2, INSTR(participantId, '-') - 2) AS INTEGER)) " +
            "FROM participant_sessions " +
            "WHERE participantId GLOB 'P[0-9][0-9]-*' OR participantId GLOB 'P[0-9][0-9][0-9]*-*'"
    )
    suspend fun getMaxParticipantSequence(): Int?

    @Query("SELECT EXISTS(SELECT 1 FROM participant_sessions WHERE participantId = :code)")
    suspend fun participantIdExists(code: String): Boolean

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

    @Query("DELETE FROM initial_user_profiles WHERE sessionId = :sessionId")
    suspend fun deleteInitialUserProfileForSession(sessionId: String)

    suspend fun deleteSessionCascade(sessionId: String) {
        deleteUserDecisionEventsForSession(sessionId)
        deleteAdaptationEventsForSession(sessionId)
        deleteInteractionEventsForSession(sessionId)
        deleteTaskRunsForSession(sessionId)
        deleteInitialUserProfileForSession(sessionId)
        deleteParticipantSession(sessionId)
        // Note: UEQ responses for this session are deleted separately via UeqDao
        // (see AppNavHost.deleteSessionCascade where both DAOs are accessible)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdaptationPreference(entity: AdaptationPreferenceEntity)

    @Query("SELECT * FROM adaptation_preferences WHERE ruleId = :ruleId AND targetLevel = :targetLevel AND scope = :scope AND taskId = :taskId LIMIT 1")
    suspend fun getAdaptationPreference(ruleId: String, targetLevel: Int, scope: String, taskId: String?): AdaptationPreferenceEntity?

    @Query("SELECT COUNT(DISTINCT taskId) FROM adaptation_preferences WHERE ruleId = :ruleId AND targetLevel = :targetLevel AND scope = 'TASK'")
    suspend fun countDistinctTasksForRejection(ruleId: String, targetLevel: Int): Int

    @Query("SELECT * FROM adaptation_preferences WHERE taskId = :taskId OR scope = 'SESSION' OR scope = 'GLOBAL'")
    suspend fun getActivePreferences(taskId: String): List<AdaptationPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskState(entity: TaskStateEntity)

    @Query("SELECT * FROM task_interaction_states WHERE taskId = :taskId AND screenId = :screenId LIMIT 1")
    suspend fun getTaskState(taskId: String, screenId: String): TaskStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInitialUserProfile(entity: InitialUserProfileEntity)

    @Query("SELECT * FROM initial_user_profiles WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getInitialUserProfile(sessionId: String): InitialUserProfileEntity?

    @Query("UPDATE adaptation_events SET undone = 1 WHERE adaptationEventId = :adaptationEventId")
    suspend fun markAdaptationEventUndone(adaptationEventId: String)

    @Query("DELETE FROM user_decision_events")
    suspend fun clearUserDecisionEvents()

    @Query("DELETE FROM adaptation_events")
    suspend fun clearAdaptationEvents()

    @Query("DELETE FROM interaction_events")
    suspend fun clearInteractionEvents()

    @Query("DELETE FROM participant_sessions")
    suspend fun deleteAllParticipantSessions()

    @Query("DELETE FROM task_runs")
    suspend fun deleteAllTaskRuns()

    @Query("DELETE FROM adaptation_preferences")
    suspend fun clearAdaptationPreferences()

    @Query("DELETE FROM task_interaction_states")
    suspend fun clearTaskInteractionStates()

    @Query("DELETE FROM initial_user_profiles")
    suspend fun clearInitialUserProfiles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskOutput(entity: TaskOutputEntity)

    @Query("SELECT * FROM task_outputs WHERE participantId = :participantId AND sessionId = :sessionId AND condition = :condition AND taskId = :taskId LIMIT 1")
    suspend fun getTaskOutput(participantId: String, sessionId: String, condition: String, taskId: String): TaskOutputEntity?

    @Query("SELECT * FROM task_outputs WHERE participantId = :participantId AND sessionId = :sessionId AND condition = :condition")
    suspend fun getTaskOutputsForSession(participantId: String, sessionId: String, condition: String): List<TaskOutputEntity>

    @Query("DELETE FROM task_outputs WHERE sessionId = :sessionId")
    suspend fun deleteTaskOutputsForSession(sessionId: String)

    @Query("DELETE FROM task_outputs")
    suspend fun clearTaskOutputs()

    suspend fun clearAll() {
        clearTaskOutputs()
        clearInitialUserProfiles()
        clearUserDecisionEvents()
        clearAdaptationEvents()
        clearInteractionEvents()
        clearAdaptationPreferences()
        clearTaskInteractionStates()
        deleteAllTaskRuns()
        deleteAllParticipantSessions()
    }

    suspend fun deleteAllResearchData() {
        clearAll()
    }
}
