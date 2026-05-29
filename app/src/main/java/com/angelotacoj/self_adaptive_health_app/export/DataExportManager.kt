package com.angelotacoj.self_adaptive_health_app.export

import android.content.Context
import android.net.Uri
import android.os.Build
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDao
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewDao
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewResponseEntity
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewStatusEntity
import com.angelotacoj.self_adaptive_health_app.ueq.persistence.UeqDao
import com.angelotacoj.self_adaptive_health_app.ueq.persistence.UeqResponseEntity
import com.angelotacoj.self_adaptive_health_app.core.logging.MapeKLog
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase C2 — Structured JSON Export Manager.
 *
 * Queries all Room DAOs, assembles the full export payload grouped by
 * participantId/sessionId, and writes it to a Uri provided by the caller
 * (typically via Storage Access Framework / ACTION_CREATE_DOCUMENT).
 *
 * Export schema version: 1
 * Fixed experiment order: STATIC_UI → UEQ → SELF_ADAPTIVE_UI → UEQ → INTERVIEW → END
 *
 * This class never modifies any production data. It is read-only with respect
 * to the Room database.
 */
class DataExportManager(
    private val experimentDao: ExperimentDao,
    private val ueqDao: UeqDao,
    private val interviewDao: InterviewDao,
    private val context: Context
) {
    sealed class ExportResult {
        object Success : ExportResult()
        data class Failure(val cause: Exception) : ExportResult()
    }

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Assembles the full export JSON and writes it to [uri].
     * Must be called from a coroutine (all DAO calls are suspend).
     */
    suspend fun exportToUri(uri: Uri): ExportResult {
        MapeKLog.experiment("export attempt uri=${uri}")
        return try {
            val json = buildExportJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("No se pudo abrir el stream de escritura para el Uri proporcionado.")
            MapeKLog.experiment("export success uri=${uri}")
            ExportResult.Success
        } catch (e: Exception) {
            MapeKLog.experiment("export failure error=${e.message}")
            ExportResult.Failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Top-level JSON assembly
    // -------------------------------------------------------------------------

    private suspend fun buildExportJson(): String {
        val root = JSONObject()

        root.put("exportMetadata", buildExportMetadata())

        // Fetch all data from Room
        val allSessions     = experimentDao.getAllParticipantSessions()
        val allTaskRuns     = experimentDao.getAllTaskRuns()
        val allTaskOutputs  = experimentDao.getAllTaskOutputs()
        val allInteraction  = experimentDao.getAllInteractionEvents()
        val allAdaptation   = experimentDao.getAllAdaptationEvents()
        val allDecisions    = experimentDao.getAllUserDecisionEvents()
        val allProfiles     = experimentDao.getAllInitialUserProfiles()
        val allUeq          = ueqDao.getAllUeqResponses()
        val allIntResponses = interviewDao.getAllResponses()
        val allIntStatuses  = interviewDao.getAllInterviewStatuses()

        // Build sessions array — each session grouped independently
        val sessionsArray = JSONArray()
        for (session in allSessions) {
            val sid = session.sessionId

            val taskRunsForSession     = allTaskRuns.filter { it.sessionId == sid }
            val taskOutputsForSession  = allTaskOutputs.filter { it.sessionId == sid }
            val interactionForSession  = allInteraction.filter { it.sessionId == sid }
            val adaptationForSession   = allAdaptation.filter { it.sessionId == sid }
            val decisionsForSession    = allDecisions.filter { it.sessionId == sid }
            val profileForSession      = allProfiles.firstOrNull { it.sessionId == sid }
            val ueqForSession          = allUeq.filter { it.sessionId == sid }
            val intResponsesForSession = allIntResponses.filter { it.sessionId == sid }
            val intStatusForSession    = allIntStatuses.firstOrNull { it.sessionId == sid }

            sessionsArray.put(
                buildSessionObject(
                    session            = session,
                    taskRuns           = taskRunsForSession,
                    taskOutputs        = taskOutputsForSession,
                    interactionEvents  = interactionForSession,
                    adaptationEvents   = adaptationForSession,
                    userDecisionEvents = decisionsForSession,
                    profile            = profileForSession,
                    ueqResponses       = ueqForSession,
                    interviewResponses = intResponsesForSession,
                    interviewStatus    = intStatusForSession
                )
            )
        }
        root.put("sessions", sessionsArray)

        // Build analysis-ready section (cross-session UEQ grouping)
        root.put("analysisReady", buildAnalysisReady(allSessions, allUeq))

        return root.toString(2) // pretty-printed, 2-space indent
    }

    // -------------------------------------------------------------------------
    // Export Metadata
    // -------------------------------------------------------------------------

    private fun buildExportMetadata(): JSONObject {
        val meta = JSONObject()
        meta.put("exportSchemaVersion", 1)
        meta.put("exportedAt", isoFormat.format(Date()))
        meta.put("appVersion", "1.0")
        meta.put("deviceInfo", buildDeviceInfo())
        meta.put("experimentDesign", "within_subjects_fixed_order")
        meta.put("conditionOrder", JSONArray().apply {
            put("STATIC_UI")
            put("SELF_ADAPTIVE_UI")
        })
        meta.put("postConditionInstrument", "official_spanish_ueq_full_26")
        meta.put("postExperimentInstrument", "short_semistructured_interview")
        meta.put("roomDatabaseVersion", 1)
        meta.put("exportScope", "all_study_data")
        return meta
    }

    private fun buildDeviceInfo(): JSONObject {
        val d = JSONObject()
        d.put("manufacturer", Build.MANUFACTURER)
        d.put("model", Build.MODEL)
        d.put("androidVersion", Build.VERSION.RELEASE)
        d.put("sdkInt", Build.VERSION.SDK_INT)
        return d
    }

    // -------------------------------------------------------------------------
    // Session object
    // -------------------------------------------------------------------------

    private fun buildSessionObject(
        session: com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity,
        taskRuns: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskRunEntity>,
        taskOutputs: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity>,
        interactionEvents: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.InteractionEventEntity>,
        adaptationEvents: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity>,
        userDecisionEvents: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity>,
        profile: com.angelotacoj.self_adaptive_health_app.core.persistence.room.InitialUserProfileEntity?,
        ueqResponses: List<UeqResponseEntity>,
        interviewResponses: List<InterviewResponseEntity>,
        interviewStatus: InterviewStatusEntity?
    ): JSONObject {
        val obj = JSONObject()
        obj.put("participantId", session.participantId)
        obj.put("sessionId", session.sessionId)
        obj.put("legacyGroup", session.group)
        obj.put("conditionOrder", session.conditionOrder)
        obj.put("sessionStartedAt", isoFormat.format(Date(session.startedAt)))
        obj.put("sessionStartedAtMs", session.startedAt)
        obj.put("fixedFlow", true)
        if (session.endedAt != null) {
            obj.put("sessionCompletedAt", isoFormat.format(Date(session.endedAt)))
            obj.put("sessionCompletedAtMs", session.endedAt)
        } else {
            obj.put("sessionCompletedAt", JSONObject.NULL)
            obj.put("sessionCompletedAtMs", JSONObject.NULL)
        }
        obj.put("isCompleted", session.isCompleted)

        // Initial profile
        obj.put("initialProfile", if (profile != null) buildInitialProfile(profile) else JSONObject.NULL)

        // Task runs
        obj.put("taskRuns", buildTaskRunsArray(taskRuns))

        // Task outputs
        obj.put("taskOutputs", buildTaskOutputsArray(taskOutputs))
        obj.put("taskOutputCompleteness", buildTaskOutputCompleteness(taskOutputs))

        // Interaction events
        obj.put("interactionEvents", buildInteractionEventsArray(interactionEvents))

        // Adaptation records — kept separate for traceability
        obj.put("adaptationEvents", buildAdaptationEventsArray(adaptationEvents))
        obj.put("userDecisionEvents", buildUserDecisionEventsArray(userDecisionEvents))

        // UEQ responses
        obj.put("ueqResponses", buildUeqResponsesArray(ueqResponses))
        obj.put("ueqCompletenessByCondition", buildUeqCompleteness(ueqResponses))

        // Interview
        obj.put("interview", buildInterview(session.participantId, session.sessionId, interviewStatus, interviewResponses))

        // Completeness summary
        obj.put("completenessSummary", buildCompletenessSummary(session, taskOutputs, ueqResponses, interviewStatus))

        return obj
    }

    // -------------------------------------------------------------------------
    // Initial profile
    // -------------------------------------------------------------------------

    private fun buildInitialProfile(
        p: com.angelotacoj.self_adaptive_health_app.core.persistence.room.InitialUserProfileEntity
    ): JSONObject {
        val obj = JSONObject()
        obj.put("participantId", p.participantId)
        obj.put("sessionId", p.sessionId)
        obj.put("timestamp", isoFormat.format(Date(p.timestamp)))
        obj.put("timestampMs", p.timestamp)
        obj.put("prefersLargeText", p.prefersLargeText)
        obj.put("prefersLargeButtons", p.prefersLargeButtons)
        obj.put("prefersIconLabels", p.prefersIconLabels)
        obj.put("prefersGuidedSteps", p.prefersGuidedSteps)
        obj.put("prefersConfirmations", p.prefersConfirmations)
        obj.put("mobileComfortLevel", p.mobileComfortLevel)
        obj.put("prefersErrorExamples", p.prefersErrorExamples)
        obj.put("prefersAdaptationPrompt", p.prefersAdaptationPrompt)
        return obj
    }

    // -------------------------------------------------------------------------
    // Task runs
    // -------------------------------------------------------------------------

    private fun buildTaskRunsArray(
        runs: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskRunEntity>
    ): JSONArray {
        val arr = JSONArray()
        for (r in runs) {
            val obj = JSONObject()
            obj.put("participantId", r.participantId)
            obj.put("sessionId", r.sessionId)
            obj.put("condition", r.condition)
            obj.put("taskId", r.taskId)
            obj.put("dataSet", r.dataSet)
            obj.put("startedAt", isoFormat.format(Date(r.startedAt)))
            obj.put("startedAtMs", r.startedAt)
            if (r.endedAt != null) {
                obj.put("completedAt", isoFormat.format(Date(r.endedAt)))
                obj.put("completedAtMs", r.endedAt)
                obj.put("totalTimeMs", r.endedAt - r.startedAt)
            } else {
                obj.put("completedAt", JSONObject.NULL)
                obj.put("completedAtMs", JSONObject.NULL)
                obj.put("totalTimeMs", JSONObject.NULL)
            }
            obj.put("completionStatus", if (r.completed) "COMPLETED" else "INCOMPLETE")
            arr.put(obj)
        }
        return arr
    }

    // -------------------------------------------------------------------------
    // Task outputs
    // -------------------------------------------------------------------------

    private val EXPECTED_TASK_OUTPUTS = listOf("T1_ACCESS", "T2_APPOINTMENT", "T3_WELL_BEING", "T4_REMINDER", "T5_SUMMARY")
    private val EXPECTED_CONDITIONS   = listOf("STATIC_UI", "SELF_ADAPTIVE_UI")

    private fun buildTaskOutputsArray(
        outputs: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity>
    ): JSONArray {
        val arr = JSONArray()
        for (o in outputs) {
            val obj = JSONObject()
            obj.put("participantId", o.participantId)
            obj.put("sessionId", o.sessionId)
            obj.put("condition", o.condition)
            obj.put("taskId", o.taskId)
            obj.put("taskOutputType", o.taskOutputType)
            obj.put("createdAt", isoFormat.format(Date(o.createdAt)))
            obj.put("createdAtMs", o.createdAt)
            obj.put("updatedAt", isoFormat.format(Date(o.updatedAt)))
            obj.put("updatedAtMs", o.updatedAt)
            // Try to parse payloadJson as nested JSON
            try {
                obj.put("payload", JSONObject(o.payloadJson))
            } catch (e1: Exception) {
                try {
                    obj.put("payload", JSONArray(o.payloadJson))
                } catch (e2: Exception) {
                    obj.put("payload", JSONObject.NULL)
                    obj.put("rawPayloadJson", o.payloadJson)
                    obj.put("parseError", e2.message ?: "JSON parse error")
                }
            }
            arr.put(obj)
        }
        return arr
    }

    private fun buildTaskOutputCompleteness(
        outputs: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity>
    ): JSONObject {
        val obj = JSONObject()
        obj.put("expectedPerCondition", JSONArray(EXPECTED_TASK_OUTPUTS))

        val byCondition = JSONArray()
        for (cond in EXPECTED_CONDITIONS) {
            val present = outputs.filter { it.condition == cond }.map { it.taskId }
            val missing = EXPECTED_TASK_OUTPUTS.filter { it !in present }
            val condObj = JSONObject()
            condObj.put("condition", cond)
            condObj.put("presentOutputs", JSONArray(present))
            condObj.put("missingOutputs", JSONArray(missing))
            condObj.put("isComplete", missing.isEmpty())
            byCondition.put(condObj)
        }
        obj.put("byCondition", byCondition)
        return obj
    }

    // -------------------------------------------------------------------------
    // Interaction events
    // -------------------------------------------------------------------------

    private fun buildInteractionEventsArray(
        events: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.InteractionEventEntity>
    ): JSONArray {
        val arr = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("eventId", e.eventId)
            obj.put("participantId", e.participantId)
            obj.put("sessionId", e.sessionId)
            obj.put("condition", e.condition)
            obj.put("taskId", e.taskId ?: JSONObject.NULL)
            obj.put("screenId", e.screenId ?: JSONObject.NULL)
            obj.put("eventType", e.eventType)
            obj.put("observedSignal", e.oisCode ?: JSONObject.NULL)
            obj.put("timestamp", isoFormat.format(Date(e.timestamp)))
            obj.put("timestampMs", e.timestamp)
            obj.put("message", e.message)
            // Parse metadata JSON if present
            if (!e.metadataJson.isNullOrBlank()) {
                try {
                    obj.put("metadata", JSONObject(e.metadataJson))
                } catch (_: Exception) {
                    obj.put("metadata", e.metadataJson)
                }
            } else {
                obj.put("metadata", JSONObject.NULL)
            }
            arr.put(obj)
        }
        return arr
    }

    // -------------------------------------------------------------------------
    // Adaptation events
    // -------------------------------------------------------------------------

    private fun buildAdaptationEventsArray(
        events: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.AdaptationEventEntity>
    ): JSONArray {
        val arr = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("adaptationEventId", e.adaptationEventId)
            obj.put("participantId", e.participantId)
            obj.put("sessionId", e.sessionId)
            obj.put("condition", e.condition)
            obj.put("taskId", e.taskId ?: JSONObject.NULL)
            obj.put("screenId", e.screenId ?: JSONObject.NULL)
            obj.put("timestamp", isoFormat.format(Date(e.timestamp)))
            obj.put("timestampMs", e.timestamp)
            obj.put("adaptationRule", e.ruleId)
            obj.put("inferredDifficulty", e.inferredDifficulty)
            obj.put("targetComponent", e.uiModifications)
            obj.put("validationType", e.validationType)
            obj.put("systemDecision", e.systemDecision)
            obj.put("userDecision", e.userDecision ?: JSONObject.NULL)
            obj.put("applied", e.applied)
            obj.put("undone", e.undone)
            arr.put(obj)
        }
        return arr
    }

    // -------------------------------------------------------------------------
    // User decision events
    // -------------------------------------------------------------------------

    private fun buildUserDecisionEventsArray(
        events: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.UserDecisionEventEntity>
    ): JSONArray {
        val arr = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("decisionId", e.decisionId)
            obj.put("participantId", e.participantId)
            obj.put("sessionId", e.sessionId)
            obj.put("adaptationEventId", e.adaptationEventId ?: JSONObject.NULL)
            obj.put("taskId", e.taskId ?: JSONObject.NULL)
            obj.put("screenId", e.screenId ?: JSONObject.NULL)
            obj.put("timestamp", isoFormat.format(Date(e.timestamp)))
            obj.put("timestampMs", e.timestamp)
            obj.put("userDecision", e.decision)
            arr.put(obj)
        }
        return arr
    }

    // -------------------------------------------------------------------------
    // UEQ responses
    // -------------------------------------------------------------------------

    private fun buildUeqResponsesArray(responses: List<UeqResponseEntity>): JSONArray {
        val arr = JSONArray()
        for (r in responses) {
            arr.put(buildUeqItem(r))
        }
        return arr
    }

    private fun buildUeqItem(r: UeqResponseEntity): JSONObject {
        val obj = JSONObject()
        obj.put("responseId", r.responseId)
        obj.put("participantId", r.participantId)
        obj.put("sessionId", r.sessionId)
        obj.put("condition", r.condition)
        obj.put("questionnaireMode", r.questionnaireMode)
        obj.put("timestamp", isoFormat.format(Date(r.timestamp)))
        obj.put("timestampMs", r.timestamp)
        obj.put("itemNumber", r.itemNumber)
        obj.put("itemId", r.itemId)
        obj.put("leftLabel", r.leftLabel)
        obj.put("rightLabel", r.rightLabel)
        obj.put("selectedValue", r.selectedValue)
        return obj
    }

    private fun buildUeqCompleteness(responses: List<UeqResponseEntity>): JSONArray {
        val arr = JSONArray()
        for (cond in EXPECTED_CONDITIONS) {
            val count = responses.count { it.condition == cond }
            val obj = JSONObject()
            obj.put("condition", cond)
            obj.put("responseCount", count)
            obj.put("expectedCount", 26)
            obj.put("isComplete", count >= 26)
            arr.put(obj)
        }
        return arr
    }

    // -------------------------------------------------------------------------
    // Interview
    // -------------------------------------------------------------------------

    private fun buildInterview(
        participantId: String,
        sessionId: String,
        status: InterviewStatusEntity?,
        responses: List<InterviewResponseEntity>
    ): JSONObject {
        val obj = JSONObject()
        obj.put("participantId", participantId)
        obj.put("sessionId", sessionId)

        val statusStr = when {
            status != null -> status.status  // "SAVED" or "SKIPPED"
            else           -> "PENDING"
        }
        obj.put("interviewStatus", statusStr)
        if (status != null) {
            obj.put("statusTimestamp", isoFormat.format(Date(status.timestamp)))
            obj.put("statusTimestampMs", status.timestamp)
        } else {
            obj.put("statusTimestamp", JSONObject.NULL)
            obj.put("statusTimestampMs", JSONObject.NULL)
        }

        val responsesArr = JSONArray()
        for (r in responses.sortedBy { it.questionNumber }) {
            val rObj = JSONObject()
            rObj.put("responseId", r.responseId)
            rObj.put("questionId", r.questionId)
            rObj.put("questionNumber", r.questionNumber)
            rObj.put("questionPrompt", r.questionPrompt)
            rObj.put("evaluatorNotes", r.evaluatorNotes)
            rObj.put("timestamp", isoFormat.format(Date(r.timestamp)))
            rObj.put("timestampMs", r.timestamp)
            responsesArr.put(rObj)
        }
        obj.put("responses", responsesArr)
        obj.put("responseCount", responses.size)
        return obj
    }

    // -------------------------------------------------------------------------
    // Completeness summary per session
    // -------------------------------------------------------------------------

    private fun buildCompletenessSummary(
        session: com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity,
        taskOutputs: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.TaskOutputEntity>,
        ueqResponses: List<UeqResponseEntity>,
        interviewStatus: InterviewStatusEntity?
    ): JSONObject {
        val obj = JSONObject()
        // Task outputs completeness per condition
        for (cond in EXPECTED_CONDITIONS) {
            val present = taskOutputs.filter { it.condition == cond }.map { it.taskId }
            val missing = EXPECTED_TASK_OUTPUTS.filter { it !in present }
            obj.put("taskOutputs_${cond}_isComplete", missing.isEmpty())
            obj.put("taskOutputs_${cond}_missing", JSONArray(missing))
        }
        // UEQ completeness per condition
        for (cond in EXPECTED_CONDITIONS) {
            val count = ueqResponses.count { it.condition == cond }
            obj.put("ueq_${cond}_responseCount", count)
            obj.put("ueq_${cond}_isComplete", count >= 26)
        }
        // Interview
        obj.put("interviewStatus", interviewStatus?.status ?: "PENDING")
        obj.put("isSessionCompleted", session.isCompleted)
        return obj
    }

    // -------------------------------------------------------------------------
    // analysisReady — cross-session UEQ grouping
    // -------------------------------------------------------------------------

    private fun buildAnalysisReady(
        allSessions: List<com.angelotacoj.self_adaptive_health_app.core.persistence.room.ParticipantSessionEntity>,
        allUeq: List<UeqResponseEntity>
    ): JSONObject {
        val root = JSONObject()

        // ueqByCondition
        val byCondition = JSONObject()
        for (cond in EXPECTED_CONDITIONS) {
            val arr = JSONArray()
            allUeq.filter { it.condition == cond }
                .sortedWith(compareBy({ it.sessionId }, { it.itemNumber }))
                .forEach { r -> arr.put(buildUeqItem(r)) }
            byCondition.put(cond, arr)
        }
        root.put("ueqByCondition", byCondition)

        // ueqPairedByParticipant + incompletePairs
        val paired = JSONArray()
        val incomplete = JSONArray()

        // Group by sessionId (one session = one participant run)
        val sessionIds = allSessions.map { it.sessionId }.distinct()
        for (sid in sessionIds) {
            val session = allSessions.first { it.sessionId == sid }
            val staticResps   = allUeq.filter { it.sessionId == sid && it.condition == "STATIC_UI" }
                                      .sortedBy { it.itemNumber }
            val adaptiveResps = allUeq.filter { it.sessionId == sid && it.condition == "SELF_ADAPTIVE_UI" }
                                      .sortedBy { it.itemNumber }

            val staticCount   = staticResps.size
            val adaptiveCount = adaptiveResps.size

            // Skip sessions with zero UEQ data entirely
            if (staticCount == 0 && adaptiveCount == 0) continue

            val isComplete = staticCount == 26 && adaptiveCount == 26

            val pairObj = JSONObject()
            pairObj.put("participantId", session.participantId)
            pairObj.put("sessionId", sid)
            pairObj.put("staticCondition", "STATIC_UI")
            pairObj.put("adaptiveCondition", "SELF_ADAPTIVE_UI")
            pairObj.put("staticResponseCount", staticCount)
            pairObj.put("adaptiveResponseCount", adaptiveCount)
            pairObj.put("isCompletePair", isComplete)

            // Compact arrays: only itemNumber, itemId, selectedValue
            pairObj.put("staticResponses", buildCompactUeqArray(staticResps))
            pairObj.put("adaptiveResponses", buildCompactUeqArray(adaptiveResps))

            // Optional mean raw scores (simple average of selectedValue 1-7)
            if (staticCount > 0) {
                val mean = staticResps.map { it.selectedValue }.average()
                pairObj.put("staticMeanRaw", mean)
            }
            if (adaptiveCount > 0) {
                val mean = adaptiveResps.map { it.selectedValue }.average()
                pairObj.put("adaptiveMeanRaw", mean)
            }
            if (staticCount > 0 && adaptiveCount > 0) {
                val diff = adaptiveResps.map { it.selectedValue }.average() -
                           staticResps.map { it.selectedValue }.average()
                pairObj.put("differenceAdaptiveMinusStatic", diff)
            }

            if (isComplete) {
                paired.put(pairObj)
            } else {
                // Also include in paired for reference but mark incomplete
                paired.put(pairObj)

                // And add to incompletePairs
                val incObj = JSONObject()
                incObj.put("participantId", session.participantId)
                incObj.put("sessionId", sid)
                incObj.put("staticResponseCount", staticCount)
                incObj.put("adaptiveResponseCount", adaptiveCount)
                if (staticCount == 0)   incObj.put("missingCondition", "STATIC_UI")
                if (adaptiveCount == 0) incObj.put("missingCondition", "SELF_ADAPTIVE_UI")
                val reason = buildList<String> {
                    if (staticCount < 26) add("STATIC_UI: $staticCount/26 responses")
                    if (adaptiveCount < 26) add("SELF_ADAPTIVE_UI: $adaptiveCount/26 responses")
                }.joinToString("; ")
                incObj.put("reason", reason)
                incomplete.put(incObj)
            }
        }
        root.put("ueqPairedByParticipant", paired)
        root.put("incompletePairs", incomplete)

        // Clarify: no statistical analysis is performed in the app
        root.put("_note", "La app no realiza análisis estadísticos. " +
            "Los datos se exportan para análisis posterior fuera del dispositivo. " +
            "No se realizan pruebas de normalidad, Shapiro-Wilk, ni t-test.")

        return root
    }

    private fun buildCompactUeqArray(responses: List<UeqResponseEntity>): JSONArray {
        val arr = JSONArray()
        for (r in responses) {
            val obj = JSONObject()
            obj.put("itemNumber", r.itemNumber)
            obj.put("itemId", r.itemId)
            obj.put("selectedValue", r.selectedValue)
            arr.put(obj)
        }
        return arr
    }
}
