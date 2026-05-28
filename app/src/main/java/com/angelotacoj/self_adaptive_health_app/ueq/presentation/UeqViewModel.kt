package com.angelotacoj.self_adaptive_health_app.ueq.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.ueq.model.UEQ_FULL_26_ITEMS
import com.angelotacoj.self_adaptive_health_app.ueq.model.UeqItem
import com.angelotacoj.self_adaptive_health_app.ueq.model.UeqMode
import com.angelotacoj.self_adaptive_health_app.ueq.persistence.UeqResponseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class UeqScreenState(
    /** All 26 official Spanish UEQ items in order. */
    val items: List<UeqItem> = UEQ_FULL_26_ITEMS,
    /** Map from itemId (e.g. "UEQ01") to selected value 1..7. Null = not yet answered. */
    val selections: Map<String, Int> = emptyMap(),
    /** Current item index shown in paged layout (0-based). */
    val currentPage: Int = 0,
    /** True while saving to Room. */
    val isSaving: Boolean = false,
    /** True after all 26 responses are successfully persisted. */
    val isSaved: Boolean = false,
    /** Error shown when user tries to submit without answering all items. */
    val validationError: String? = null,
    /** True if the user attempted to submit at least once. */
    val submitAttempted: Boolean = false,
    /** True if the questionnaire is already completed in the database. */
    val alreadyCompleted: Boolean = false
) {
    val totalItems: Int get() = items.size
    val answeredCount: Int get() = selections.size
    val allAnswered: Boolean get() = selections.size == totalItems
    val currentItem: UeqItem? get() = items.getOrNull(currentPage)
    val isLastPage: Boolean get() = currentPage == items.lastIndex
    val currentSelection: Int? get() = currentItem?.let { selections[it.id] }
}

sealed class UeqEvent {
    /** User tapped a value on the scale (1..7) for the current item. */
    data class SelectValue(val itemId: String, val value: Int) : UeqEvent()
    /** User wants to go to the next item. */
    object NextPage : UeqEvent()
    /** User wants to go back to the previous item. */
    object PreviousPage : UeqEvent()
    /** User pressed the final submit/save button. */
    object Submit : UeqEvent()
    /** Clear any validation error banner. */
    object DismissError : UeqEvent()
    /** User acknowledges the UEQ is already completed and wants to continue. */
    object AcknowledgeAlreadyCompleted : UeqEvent()
}

class UeqViewModel : ViewModel() {

    private val _state = MutableStateFlow(UeqScreenState())
    val state: StateFlow<UeqScreenState> = _state.asStateFlow()

    // Session metadata – set once when the screen is opened.
    private var participantId: String = ""
    private var sessionId: String = ""
    private var group: ExperimentGroup = ExperimentGroup.GroupA
    private var condition: ExperimentCondition = ExperimentCondition.STATIC_UI
    private var onSaved: () -> Unit = {}

    /**
     * Called from the nav host right after the screen is composed.
     * [onSaved] is invoked on the Main thread when all 26 responses are persisted.
     */
    fun init(
        participantId: String,
        sessionId: String,
        group: ExperimentGroup,
        condition: ExperimentCondition,
        onSaved: () -> Unit
    ) {
        this.participantId = participantId
        this.sessionId = sessionId
        this.group = group
        this.condition = condition
        this.onSaved = onSaved

        viewModelScope.launch {
            val count = com.angelotacoj.self_adaptive_health_app.di.AppContainer.database.ueqDao().countAnsweredItems(
                participantId = participantId,
                sessionId = sessionId,
                condition = condition.name,
                questionnaireMode = "UEQ_FULL_26"
            )
            if (count >= 26) {
                _state.update { it.copy(alreadyCompleted = true) }
            }
        }
    }

    fun onEvent(event: UeqEvent) {
        when (event) {
            is UeqEvent.SelectValue -> {
                _state.update { s ->
                    s.copy(
                        selections = s.selections + (event.itemId to event.value),
                        validationError = null
                    )
                }
            }

            is UeqEvent.NextPage -> {
                _state.update { s ->
                    if (s.currentPage < s.items.lastIndex)
                        s.copy(currentPage = s.currentPage + 1, validationError = null)
                    else s
                }
            }

            is UeqEvent.PreviousPage -> {
                _state.update { s ->
                    if (s.currentPage > 0)
                        s.copy(currentPage = s.currentPage - 1, validationError = null)
                    else s
                }
            }

            is UeqEvent.Submit -> handleSubmit()

            is UeqEvent.DismissError -> {
                _state.update { it.copy(validationError = null) }
            }
            
            is UeqEvent.AcknowledgeAlreadyCompleted -> {
                _state.update { it.copy(isSaved = true) }
            }
        }
    }

    private fun handleSubmit() {
        _state.update { it.copy(submitAttempted = true) }
        val current = _state.value
        if (!current.allAnswered) {
            val missing = current.totalItems - current.answeredCount
            _state.update {
                it.copy(
                    validationError = "Faltan $missing ítem(s) por responder. Por favor responda todos los ítems antes de continuar."
                )
            }
            return
        }
        save(current)
    }

    private fun save(s: UeqScreenState) {
        if (s.isSaving || s.isSaved) return
        _state.update { it.copy(isSaving = true, validationError = null) }
        val now = System.currentTimeMillis()
        val entities = s.items.map { item ->
            UeqResponseEntity(
                responseId = "${sessionId}_${condition.name}_${item.id}",
                participantId = participantId,
                sessionId = sessionId,
                group = group.name,
                condition = condition.name,
                questionnaireMode = UeqMode.UEQ_FULL_26.name,
                timestamp = now,
                itemId = item.id,
                itemNumber = item.number,
                leftLabel = item.leftLabel,
                rightLabel = item.rightLabel,
                selectedValue = s.selections[item.id] ?: 4   // fallback centre; never reached due to validation
            )
        }
        viewModelScope.launch {
            try {
                AppContainer.database.ueqDao().insertAllUeqResponses(entities)
                _state.update { it.copy(isSaving = false, isSaved = true) }
                onSaved()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        validationError = "Error al guardar. Inténtelo de nuevo."
                    )
                }
            }
        }
    }
}
