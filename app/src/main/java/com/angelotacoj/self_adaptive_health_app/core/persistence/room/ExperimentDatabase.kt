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
    version = 2,
    exportSchema = false
)
abstract class ExperimentDatabase : RoomDatabase() {
    abstract fun experimentDao(): ExperimentDao

    companion object {
        @Volatile private var instance: ExperimentDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Drop the old table and recreate since we changed primary keys and columns
                db.execSQL("DROP TABLE IF EXISTS `adaptation_preferences`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `adaptation_preferences` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ruleId` TEXT NOT NULL,
                        `targetLevel` INTEGER NOT NULL,
                        `scope` TEXT NOT NULL,
                        `taskId` TEXT,
                        `screenId` TEXT,
                        `rejectedCount` INTEGER NOT NULL,
                        `lastRejectedAt` INTEGER NOT NULL,
                        `suppressAutomatic` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_adaptation_preferences_ruleId_targetLevel_scope_taskId` ON `adaptation_preferences` (`ruleId`, `targetLevel`, `scope`, `taskId`)")
            }
        }

        fun getInstance(context: Context): ExperimentDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExperimentDatabase::class.java,
                    "aura_experiment.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
        }
    }
}
