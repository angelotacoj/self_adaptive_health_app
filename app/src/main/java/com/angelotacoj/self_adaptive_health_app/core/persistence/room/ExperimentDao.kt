package com.angelotacoj.self_adaptive_health_app.core.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExperimentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipantSession(entity: ParticipantSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskRun(entity: TaskRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionEvent(entity: InteractionEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdaptationEvent(entity: AdaptationEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserDecisionEvent(entity: UserDecisionEventEntity)

    @Query("SELECT * FROM interaction_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentInteractionEvents(limit: Int = 200): List<InteractionEventEntity>

    @Query("SELECT * FROM adaptation_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentAdaptationEvents(limit: Int = 200): List<AdaptationEventEntity>

    @Query("DELETE FROM interaction_events")
    suspend fun clearInteractionEvents()

    @Query("DELETE FROM adaptation_events")
    suspend fun clearAdaptationEvents()

    @Query("DELETE FROM user_decision_events")
    suspend fun clearUserDecisionEvents()
}
