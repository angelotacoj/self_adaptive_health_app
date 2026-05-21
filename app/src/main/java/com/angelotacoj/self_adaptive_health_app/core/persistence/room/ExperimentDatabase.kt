package com.angelotacoj.self_adaptive_health_app.core.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ParticipantSessionEntity::class,
        TaskRunEntity::class,
        InteractionEventEntity::class,
        AdaptationEventEntity::class,
        UserDecisionEventEntity::class,
        AdaptationPreferenceEntity::class,
        TaskStateEntity::class,
        InitialUserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExperimentDatabase : RoomDatabase() {
    abstract fun experimentDao(): ExperimentDao

    companion object {
        @Volatile private var instance: ExperimentDatabase? = null

        fun getInstance(context: Context): ExperimentDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExperimentDatabase::class.java,
                    "aura_experiment.db"
                ).build().also { instance = it }
            }
        }
    }
}
