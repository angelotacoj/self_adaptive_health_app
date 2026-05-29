package com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
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

class PersistentKnowledgeRepository(
    private val preferences: ExperimentPreferences,
    private val dao: ExperimentDao
) : InMemoryKnowledgeRepository() {
    override suspend fun getCurrentSession(): String? {
        val snapshot = preferences.sessionSnapshot.first()
        // Use the persisted sessionId directly — it is set by AppNavHost when the session starts.
        // The previous construction ("${participantId}_${dataSet}") was incorrect and caused
        // interaction events to be stored under a key that never matched any participant_sessions row.
        return snapshot.currentSessionId
    }

    override suspend fun getCurrentCondition(): String? {
        return preferences.sessionSnapshot.first().currentCondition
    }

    override fun snapshot(taskId: TaskId?, screenId: ScreenId?): KnowledgeSnapshot {
        return super.snapshot(taskId, screenId)
    }

    override suspend fun clearTask(taskId: TaskId?) {
        super.clearTask(taskId)
        if (taskId != null) {
            val prefs = dao.getActivePreferences(taskId.name)
            prefs.forEach { pref ->
                if (pref.suppressAutomatic) {
                    try {
                        val rule = AdaptationRuleId.valueOf(pref.ruleId)
                        val level = AdaptationLevel.entries.find { it.levelValue == pref.targetLevel } ?: AdaptationLevel.LEVEL_1_LIGHT_SUPPORT
                        val levels = rejectedLevelsByTask.getOrPut(taskId) { mutableSetOf() }
                        levels.add(Pair(rule, level))
                        val rules = rejectedByTask.getOrPut(taskId) { mutableSetOf() }
                        rules.add(rule)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    override fun rememberRejected(
        taskId: TaskId?,
        ruleId: AdaptationRuleId,
        level: AdaptationLevel
    ) {
        super.rememberRejected(taskId, ruleId, level)
        if (taskId != null) {
            MainScope().launch(Dispatchers.IO) {
                val existing = dao.getAdaptationPreference(ruleId.name, level.levelValue, com.angelotacoj.self_adaptive_health_app.core.persistence.room.RejectionScope.TASK.name, taskId.name)
                val newRejectedCount = (existing?.rejectedCount ?: 0) + 1
                dao.insertAdaptationPreference(
                    AdaptationPreferenceEntity(
                        ruleId = ruleId.name,
                        targetLevel = level.levelValue,
                        scope = com.angelotacoj.self_adaptive_health_app.core.persistence.room.RejectionScope.TASK.name,
                        taskId = taskId.name,
                        screenId = null,
                        rejectedCount = newRejectedCount,
                        lastRejectedAt = System.currentTimeMillis(),
                        suppressAutomatic = newRejectedCount >= 2
                    )
                )

                // Check for SESSION promotion: at least two DIFFERENT tasks rejected this rule + level
                val distinctTaskCount = dao.countDistinctTasksForRejection(ruleId.name, level.levelValue)
                if (distinctTaskCount >= 2) {
                    val existingSession = dao.getAdaptationPreference(ruleId.name, level.levelValue, com.angelotacoj.self_adaptive_health_app.core.persistence.room.RejectionScope.SESSION.name, null)
                    if (existingSession == null) {
                        dao.insertAdaptationPreference(
                            AdaptationPreferenceEntity(
                                ruleId = ruleId.name,
                                targetLevel = level.levelValue,
                                scope = com.angelotacoj.self_adaptive_health_app.core.persistence.room.RejectionScope.SESSION.name,
                                taskId = null,
                                screenId = null,
                                rejectedCount = 1,
                                lastRejectedAt = System.currentTimeMillis(),
                                suppressAutomatic = true
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun saveInteractionEvent(entry: DebugLogEntry, oisCode: String?) {
        dao.insertInteractionEvent(
            InteractionEventEntity(
                eventId = entry.id,
                sessionId = getCurrentSession() ?: "local_session",
                participantId = entry.participantId,
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
