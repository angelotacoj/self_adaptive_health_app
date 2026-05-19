package com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.persistence.datastore.ExperimentPreferences
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDao
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.InteractionEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity
import kotlinx.coroutines.flow.first

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

    override suspend fun wasRejectedInCurrentTask(taskId: com.angelotacoj.self_adaptive_health_app.core.logging.TaskId?, ruleId: AdaptationRuleId): Boolean {
        return super.wasRejectedInCurrentTask(taskId, ruleId)
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

    override suspend fun clearCurrentSession() {
        preferences.clearSession()
        clearCurrentTaskAdaptationMemory()
    }
}
