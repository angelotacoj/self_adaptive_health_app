package com.angelotacoj.self_adaptive_health_app.core.persistence.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.experimentDataStore by preferencesDataStore(name = "experiment_preferences")

data class SessionPreferenceSnapshot(
    val currentSessionId: String? = null,
    val participantId: String? = null,
    val group: String? = null,
    val currentCondition: String? = null,
    val currentConditionIndex: Int = 0,
    val currentDataSet: String? = null,
    val isSessionActive: Boolean = false,
    val isProfileCompleted: Boolean = false
)

class ExperimentPreferences(private val context: Context) {
    val sessionSnapshot: Flow<SessionPreferenceSnapshot> = context.experimentDataStore.data.map { prefs ->
        SessionPreferenceSnapshot(
            currentSessionId = prefs[CURRENT_SESSION_ID],
            participantId = prefs[PARTICIPANT_CODE],
            group = prefs[GROUP],
            currentCondition = prefs[CURRENT_CONDITION],
            currentConditionIndex = prefs[CURRENT_CONDITION_INDEX] ?: 0,
            currentDataSet = prefs[CURRENT_DATA_SET],
            isSessionActive = prefs[IS_SESSION_ACTIVE] ?: false,
            isProfileCompleted = prefs[IS_PROFILE_COMPLETED] ?: false
        )
    }

    suspend fun saveSession(state: ExperimentSessionState) {
        context.experimentDataStore.edit { prefs ->
            prefs[CURRENT_SESSION_ID] = state.sessionId
            prefs[PARTICIPANT_CODE] = state.participantId
            prefs[GROUP] = state.group.name
            prefs[CURRENT_CONDITION] = state.currentCondition.name
            prefs[CURRENT_CONDITION_INDEX] = state.currentConditionIndex
            prefs[CURRENT_DATA_SET] = state.currentDataSet.id
            prefs[IS_SESSION_ACTIVE] = state.isSessionActive
            prefs[IS_PROFILE_COMPLETED] = state.isProfileCompleted
        }
    }

    suspend fun markProfileCompleted() {
        context.experimentDataStore.edit { prefs ->
            prefs[IS_PROFILE_COMPLETED] = true
        }
    }

    suspend fun clearSession() {
        clearActiveSessionPreferences()
    }

    suspend fun clearActiveSessionPreferences() {
        context.experimentDataStore.edit { prefs ->
            prefs.remove(CURRENT_SESSION_ID)
            prefs.remove(PARTICIPANT_CODE)
            prefs.remove(GROUP)
            prefs.remove(CURRENT_CONDITION)
            prefs.remove(CURRENT_CONDITION_INDEX)
            prefs.remove(CURRENT_DATA_SET)
            prefs.remove(IS_PROFILE_COMPLETED)
            prefs[IS_SESSION_ACTIVE] = false
        }
    }

    private companion object {
        val CURRENT_SESSION_ID = stringPreferencesKey("current_session_id")
        val PARTICIPANT_CODE = stringPreferencesKey("participant_code")
        val GROUP = stringPreferencesKey("group")
        val CURRENT_CONDITION = stringPreferencesKey("current_condition")
        val CURRENT_CONDITION_INDEX = intPreferencesKey("current_condition_index")
        val CURRENT_DATA_SET = stringPreferencesKey("current_data_set")
        val IS_SESSION_ACTIVE = booleanPreferencesKey("is_session_active")
        val IS_PROFILE_COMPLETED = booleanPreferencesKey("is_profile_completed")
    }
}
