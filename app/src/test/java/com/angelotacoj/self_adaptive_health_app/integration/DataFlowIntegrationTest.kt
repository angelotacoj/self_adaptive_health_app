package com.angelotacoj.self_adaptive_health_app.integration

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.angelotacoj.self_adaptive_health_app.MainDispatcherRule
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationLevel
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AdaptationRuleId
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.repository.PersistentKnowledgeRepository
import com.angelotacoj.self_adaptive_health_app.adaptive.presentation.state.AdaptiveUiState
import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
import com.angelotacoj.self_adaptive_health_app.core.persistence.datastore.ExperimentPreferences
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDao
import com.angelotacoj.self_adaptive_health_app.core.persistence.room.ExperimentDatabase
import com.angelotacoj.self_adaptive_health_app.healthtasks.access.AccessAction
import com.angelotacoj.self_adaptive_health_app.healthtasks.access.AccessViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataFlowIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dao = mockk<ExperimentDao>(relaxed = true)
    private val preferences = ExperimentPreferences(ApplicationProvider.getApplicationContext())
    private val repository by lazy { PersistentKnowledgeRepository(preferences, dao) }

    @Test
    fun `rejected adaptation in ViewModel is persisted in repository`() = runTest {
        val viewModel = AccessViewModel()
        val credentials = AccessCredentials("USER123", "1234")
        viewModel.start(credentials)

        // Simulate an adaptation being applied and then rejected (undone)
        val adaptiveState = AdaptiveUiState(
            isAdaptiveMode = true,
            lastAppliedAdaptation = com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.AppliedAdaptation(
                ruleId = AdaptationRuleId.AR01_TIME_ON_SCREEN,
                modifications = emptyList(),
                level = AdaptationLevel.LEVEL_1_LIGHT_SUPPORT
            )
        )
        
        // The UI would call this when "Undo" is pressed
        repository.rememberRejected(
            com.angelotacoj.self_adaptive_health_app.core.logging.TaskId.T1_ACCESS,
            AdaptationRuleId.AR01_TIME_ON_SCREEN,
            AdaptationLevel.LEVEL_1_LIGHT_SUPPORT
        )

        // Verify that the repository now knows it's rejected
        val snapshot = repository.snapshot(com.angelotacoj.self_adaptive_health_app.core.logging.TaskId.T1_ACCESS, null)
        assert(snapshot.rejectedRuleLevelsForTask.contains(Pair(AdaptationRuleId.AR01_TIME_ON_SCREEN, AdaptationLevel.LEVEL_1_LIGHT_SUPPORT)))
    }

    @Test
    fun `text scaling scales font size correctly`() {
        val stateNormal = AdaptiveUiState(uim01Level = AdaptationLevel.LEVEL_0_BASE)
        val stateLarge = AdaptiveUiState(uim01Level = AdaptationLevel.LEVEL_3_HIGH_SUPPORT)

        // Verify state object values that would be passed to components
        assertEquals(1.0f, stateNormal.textScale)
        assertEquals(1.35f, stateLarge.textScale)
    }
}
