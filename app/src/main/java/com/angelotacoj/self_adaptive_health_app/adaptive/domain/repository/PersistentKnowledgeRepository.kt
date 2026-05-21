package com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.KnowledgeSnapshot
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.persistence.datastore.ExperimentPreferences
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationPreferenceEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDao
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.InteractionEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.InitialUserProfileEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskStateEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PersistentKnowledgeRepository(
    private val preferences: ExperimentPreferences,
    private val dao: ExperimentDao
) : InMemoryKnowledgeRepository() {
    override suspend fun getCurrentSession(): String? {
        val snapshot = preferences.sessionSnapshot.first()
        return snapshot.participantCode?.let { "${it}_${snapshot.currentDataSet ?: "dataset"}" }
    }

    override suspend fun getCurrentCondition(): String? {
        return preferences.sessionSnapshot.first().currentCondition
    }

    override fun snapshot(taskId: TaskId?, screenId: ScreenId?): KnowledgeSnapshot {
        if (taskId != null) {
            runBlocking(Dispatchers.IO) {
                val prefs = dao.getPreferencesForTask(taskId.name)
                prefs.forEach { pref ->
                    if (pref.suppressAutomatic) {
                        val rules = rejectedByTask.getOrPut(taskId) { mutableSetOf() }
                        rules.add(AdaptationRuleId.valueOf(pref.ruleId))
                    }
                }
            }
        }
        return super.snapshot(taskId, screenId)
    }

    override fun rememberRejected(taskId: TaskId?, ruleId: AdaptationRuleId) {
        super.rememberRejected(taskId, ruleId)
        if (taskId != null) {
            MainScope().launch(Dispatchers.IO) {
                val existing = dao.getAdaptationPreference(ruleId.name, taskId.name, "ANY")
                val newRejectedCount = (existing?.rejectedCount ?: 0) + 1
                dao.insertAdaptationPreference(
                    AdaptationPreferenceEntity(
                        ruleId = ruleId.name,
                        taskId = taskId.name,
                        screenId = "ANY",
                        rejectedCount = newRejectedCount,
                        lastRejectedAt = System.currentTimeMillis(),
                        suppressAutomatic = newRejectedCount >= 2
                    )
                )
            }
        }
    }

    override suspend fun saveInteractionEvent(entry: DebugLogEntry, oisCode: String?) {
        dao.insertInteractionEvent(
            InteractionEventEntity(
                eventId = entry.id,
                sessionId = getCurrentSession() ?: "local_session",
                participantCode = entry.participantCode,
                condition = entry.condition.name,
                taskId = entry.taskId?.name,
                screenId = entry.screenId?.name,
                eventType = entry.eventType.name,
                oisCode = oisCode,
                timestamp = entry.timestamp,
                message = entry.message,
                metadataJson = entry.metadata.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":\"${it.value}\"" }
            )
        )
    }

    override suspend fun saveAdaptationEvent(entity: AdaptationEventEntity) {
        dao.insertAdaptationEvent(entity)
    }

    override suspend fun saveUserDecision(entity: UserDecisionEventEntity) {
        dao.insertUserDecisionEvent(entity)
    }

    override suspend fun markUndone(adaptationEventId: String) {
        dao.markAdaptationEventUndone(adaptationEventId)
    }

    override suspend fun saveTaskState(entity: TaskStateEntity) {
        dao.insertTaskState(entity)
    }

    override suspend fun getTaskState(taskId: TaskId, screenId: ScreenId): TaskStateEntity? {
        return dao.getTaskState(taskId.name, screenId.name)
    }

    override suspend fun saveInitialUserProfile(entity: InitialUserProfileEntity) {
        dao.insertInitialUserProfile(entity)
    }

    override suspend fun clearCurrentSession() {
        preferences.clearSession()
        clearCurrentTaskAdaptationMemory()
    }
}
