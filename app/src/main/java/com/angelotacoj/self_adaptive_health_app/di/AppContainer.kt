package com.angelotacoj.self_adaptive_health_app.di

import android.content.Context
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.KnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.PersistentKnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.core.data.FakeHealthDataSource
import com.angelotacoj.self_adaptive_health_app.core.logging.InMemoryExperimentLogger
import com.angelotacoj.self_adaptive_health_app.core.persistence.datastore.ExperimentPreferences
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDatabase

object AppContainer {
    val fakeHealthDataSource = FakeHealthDataSource()
    val experimentLogger = InMemoryExperimentLogger()
    lateinit var experimentPreferences: ExperimentPreferences
        private set
    lateinit var database: ExperimentDatabase
        private set
    lateinit var knowledgeRepository: KnowledgeRepository
        private set

    fun init(context: Context) {
        if (::database.isInitialized) return
        database = ExperimentDatabase.getInstance(context)
        experimentPreferences = ExperimentPreferences(context)
        knowledgeRepository = PersistentKnowledgeRepository(experimentPreferences, database.experimentDao())
    }
}
