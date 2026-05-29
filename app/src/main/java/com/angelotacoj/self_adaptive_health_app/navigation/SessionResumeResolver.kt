package com.angelotacoj.self_adaptive_health_app.navigation

import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentTaskOrder
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDao
import com.angelotacoj.self_adaptive_health_app.ueq.persistence.UeqDao
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewDao

data class PendingStepResult(
    val route: String,
    val targetConditionIndex: Int,
    val isUeq: Boolean = false,
    val isInterview: Boolean = false,
    val isCompleted: Boolean = false
)

object SessionResumeResolver {
    suspend fun resolvePendingStep(
        session: ExperimentSessionState,
        experimentDao: ExperimentDao,
        ueqDao: UeqDao,
        interviewDao: InterviewDao
    ): PendingStepResult {
        val isProfileCompleted = experimentDao.getInitialUserProfile(session.sessionId) != null
        if (!isProfileCompleted) {
            return PendingStepResult(AppRoute.InitialProfile.route, 0)
        }

        val cond0 = session.conditionOrder.getOrNull(0)
        val cond1 = session.conditionOrder.getOrNull(1)

        // Check Condition 0 (STATIC)
        if (cond0 != null) {
            val tasks0Done = session.completedTasksByCondition[cond0]?.size == ExperimentTaskOrder.size
            val ueq0Count = ueqDao.countAnsweredItems(session.participantId, session.sessionId, cond0.name, "UEQ_FULL_26")
            val ueq0Done = ueq0Count >= 26

            if (!tasks0Done) {
                return PendingStepResult(AppRoute.Home.route, 0)
            } else if (!ueq0Done) {
                return PendingStepResult(AppRoute.Ueq.route, 0, isUeq = true)
            }
        }

        // Check Condition 1 (ADAPTIVE)
        if (cond1 != null) {
            val tasks1Done = session.completedTasksByCondition[cond1]?.size == ExperimentTaskOrder.size
            val ueq1Count = ueqDao.countAnsweredItems(session.participantId, session.sessionId, cond1.name, "UEQ_FULL_26")
            val ueq1Done = ueq1Count >= 26

            if (!tasks1Done) {
                return PendingStepResult(AppRoute.Home.route, 1)
            } else if (!ueq1Done) {
                return PendingStepResult(AppRoute.Ueq.route, 1, isUeq = true)
            }
        }

        // Check Interview – use persisted status as source of truth.
        // A row in interview_status with SAVED or SKIPPED means the interview is done.
        // No row (null) = PENDING: resume at InterviewScreen.
        val interviewStatus = interviewDao.getInterviewStatus(session.participantId, session.sessionId)
        if (interviewStatus == null) {
            return PendingStepResult(AppRoute.Interview.route, 1, isInterview = true)
        }

        // Interview done (SAVED or SKIPPED) → session completed
        return PendingStepResult(AppRoute.SessionCompleted.route, 1, isCompleted = true)
    }
}
