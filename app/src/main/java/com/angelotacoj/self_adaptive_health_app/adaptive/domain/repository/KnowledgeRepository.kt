package com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.KnowledgeSnapshot
import com.angelotacoj.self_adaptive_health_app.core.logging.DebugLogEntry
import com.angelotacoj.self_adaptive_health_app.core.logging.ScreenId
import com.angelotacoj.self_adaptive_health_app.core.logging.TaskId
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskStateEntity
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity

interface KnowledgeRepository {
    suspend fun getCurrentSession(): String?
    suspend fun getCurrentCondition(): String?
    suspend fun wasRejectedInCurrentTask(taskId: TaskId?, ruleId: AdaptationRuleId): Boolean
    fun rememberRejected(taskId: TaskId?, ruleId: AdaptationRuleId)
    fun rememberSuggested(taskId: TaskId?, ruleId: AdaptationRuleId)
    fun rememberModal(screenId: ScreenId?, ruleId: AdaptationRuleId)
    fun snapshot(taskId: TaskId?, screenId: ScreenId?): KnowledgeSnapshot
    suspend fun saveInteractionEvent(entry: DebugLogEntry, oisCode: String?)
    suspend fun saveAdaptationEvent(entity: AdaptationEventEntity)
    suspend fun saveUserDecision(entity: UserDecisionEventEntity)
    suspend fun clearCurrentSession()
    fun clearTask(taskId: TaskId?)
    fun clearCurrentTaskAdaptationMemory()
    suspend fun markUndone(adaptationEventId: String)
    suspend fun saveTaskState(entity: TaskStateEntity)
    suspend fun getTaskState(taskId: TaskId, screenId: ScreenId): TaskStateEntity?
}

open class InMemoryKnowledgeRepository : KnowledgeRepository {
    protected val rejectedByTask = mutableMapOf<TaskId, MutableSet<AdaptationRuleId>>()
    protected val suggestedCountByTask = mutableMapOf<TaskId, Int>()
    protected val modalShownByScreen = mutableMapOf<ScreenId, Boolean>()
    protected var lastRejectionAt: Long? = null

    override suspend fun getCurrentSession(): String? = null
    override suspend fun getCurrentCondition(): String? = null
    override suspend fun wasRejectedInCurrentTask(taskId: TaskId?, ruleId: AdaptationRuleId): Boolean {
        return taskId?.let { rejectedByTask[it]?.contains(ruleId) } ?: false
    }

    override fun rememberRejected(taskId: TaskId?, ruleId: AdaptationRuleId) {
        if (taskId != null) {
            val rules = rejectedByTask.getOrPut(taskId) { mutableSetOf() }
            rules.add(ruleId)
            lastRejectionAt = System.currentTimeMillis()
        }
    }

    override fun rememberSuggested(taskId: TaskId?, ruleId: AdaptationRuleId) {
        if (taskId != null) {
            suggestedCountByTask[taskId] = (suggestedCountByTask[taskId] ?: 0) + 1
        }
    }

    override fun rememberModal(screenId: ScreenId?, ruleId: AdaptationRuleId) {
        if (screenId != null) {
            modalShownByScreen[screenId] = true
        }
    }

    override fun snapshot(taskId: TaskId?, screenId: ScreenId?): KnowledgeSnapshot {
        return KnowledgeSnapshot(
            rejectedRulesForTask = taskId?.let { rejectedByTask[it]?.toSet() } ?: emptySet(),
            suggestionsShownForTask = taskId?.let { suggestedCountByTask[it] } ?: 0,
            modalShownForScreen = screenId?.let { modalShownByScreen[it] } ?: false,
            lastRejectionAt = lastRejectionAt
        )
    }

    override suspend fun saveInteractionEvent(entry: DebugLogEntry, oisCode: String?) {}
    override suspend fun saveAdaptationEvent(entity: AdaptationEventEntity) {}
    override suspend fun saveUserDecision(entity: UserDecisionEventEntity) {}
    override suspend fun clearCurrentSession() { clearCurrentTaskAdaptationMemory() }
    override fun clearTask(taskId: TaskId?) {
        if (taskId != null) {
            rejectedByTask.remove(taskId)
            suggestedCountByTask.remove(taskId)
        }
    }

    override fun clearCurrentTaskAdaptationMemory() {
        rejectedByTask.clear()
        suggestedCountByTask.clear()
        modalShownByScreen.clear()
        lastRejectionAt = null
    }

    override suspend fun markUndone(adaptationEventId: String) {}
    override suspend fun saveTaskState(entity: TaskStateEntity) {}
    override suspend fun getTaskState(taskId: TaskId, screenId: ScreenId): TaskStateEntity? = null
}