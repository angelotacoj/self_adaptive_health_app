package com.angelotacoj.self_adaptive_health_app.core.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewDao
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewResponseEntity
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewStatusEntity
import com.angelotacoj.self_adaptive_health_app.ueq.persistence.UeqDao
import com.angelotacoj.self_adaptive_health_app.ueq.persistence.UeqResponseEntity

@Database(
    entities = [
        ParticipantSessionEntity::class,
        TaskRunEntity::class,
        InteractionEventEntity::class,
        AdaptationEventEntity::class,
        UserDecisionEventEntity::class,
        AdaptationPreferenceEntity::class,
        TaskStateEntity::class,
        InitialUserProfileEntity::class,
        TaskOutputEntity::class,
        UeqResponseEntity::class,              // Phase C1: UEQ full 26-item responses
        InterviewResponseEntity::class,        // Phase C1.5: short semi-structured interview
        InterviewStatusEntity::class           // Phase C1.5: interview completion status (SAVED/SKIPPED)
    ],
    version = 1,
    exportSchema = false
)
abstract class ExperimentDatabase : RoomDatabase() {
    abstract fun experimentDao(): ExperimentDao
    abstract fun ueqDao(): UeqDao              // Phase C1
    abstract fun interviewDao(): InterviewDao  // Phase C1.5

    companion object {
        @Volatile private var instance: ExperimentDatabase? = null

        fun getInstance(context: Context): ExperimentDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExperimentDatabase::class.java,
                    "experiment_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
