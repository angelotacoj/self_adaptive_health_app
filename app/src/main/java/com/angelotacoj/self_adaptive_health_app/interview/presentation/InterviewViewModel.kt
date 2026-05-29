package com.angelotacoj.self_adaptive_health_app.interview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.interview.model.INTERVIEW_QUESTIONS
import com.angelotacoj.self_adaptive_health_app.interview.model.InterviewQuestion
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewResponseEntity
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewStatusEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterviewScreenState(
    val questions: List<InterviewQuestion> = INTERVIEW_QUESTIONS,
    /** Map from questionId to the evaluator's free-text notes. */
    val notes: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null,
    /** Show the "all notes empty, confirm finalization" dialog. */
    val showEmptyConfirmDialog: Boolean = false,
    /** Show the "confirm skip" dialog. */
    val showSkipConfirmDialog: Boolean = false
) {
    val currentQuestion: InterviewQuestion? get() = questions.firstOrNull()
    val answeredCount: Int get() = notes.values.count { it.isNotBlank() }
    val totalQuestions: Int get() = questions.size
    val allNotesEmpty: Boolean get() = notes.values.all { it.isBlank() }
}

sealed class InterviewEvent {
    data class NotesChanged(val questionId: String, val text: String) : InterviewEvent()
    /** Evaluator presses "Guardar entrevista" */
    object Save : InterviewEvent()
    /** Evaluator confirms saving even with all notes empty */
    object ConfirmEmptySave : InterviewEvent()
    /** Evaluator presses "Volver" from empty-save dialog */
    object DismissEmptySaveDialog : InterviewEvent()
    /** Evaluator presses "Omitir entrevista" */
    object Skip : InterviewEvent()
    /** Evaluator confirms skip */
    object ConfirmSkip : InterviewEvent()
    /** Evaluator presses "Volver" from skip dialog */
    object DismissSkipDialog : InterviewEvent()
    object DismissError : InterviewEvent()
}

class InterviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(InterviewScreenState())
    val state: StateFlow<InterviewScreenState> = _state.asStateFlow()

    private var participantId: String = ""
    private var sessionId: String = ""
    private var onFinished: () -> Unit = {}

    /**
     * Called from the nav host when the screen is first composed.
     * [onFinished] is invoked on Main after save or skip is fully persisted.
     */
    fun init(participantId: String, sessionId: String, onFinished: () -> Unit) {
        this.participantId = participantId
        this.sessionId = sessionId
        this.onFinished = onFinished
    }

    fun onEvent(event: InterviewEvent) {
        when (event) {
            is InterviewEvent.NotesChanged -> {
                _state.update { s ->
                    s.copy(notes = s.notes + (event.questionId to event.text), saveError = null)
                }
            }

            is InterviewEvent.Save -> {
                val current = _state.value
                if (current.isSaving || current.isSaved) return
                if (current.allNotesEmpty) {
                    // Show empty-save confirmation dialog instead of silently saving
                    _state.update { it.copy(showEmptyConfirmDialog = true) }
                } else {
                    persistSave()
                }
            }

            is InterviewEvent.ConfirmEmptySave -> {
                _state.update { it.copy(showEmptyConfirmDialog = false) }
                persistSave()
            }

            is InterviewEvent.DismissEmptySaveDialog -> {
                _state.update { it.copy(showEmptyConfirmDialog = false) }
            }

            is InterviewEvent.Skip -> {
                if (_state.value.isSaved || _state.value.isSaving) return
                // Show skip confirmation dialog
                _state.update { it.copy(showSkipConfirmDialog = true) }
            }

            is InterviewEvent.ConfirmSkip -> {
                _state.update { it.copy(showSkipConfirmDialog = false) }
                persistSkip()
            }

            is InterviewEvent.DismissSkipDialog -> {
                _state.update { it.copy(showSkipConfirmDialog = false) }
            }

            is InterviewEvent.DismissError -> {
                _state.update { it.copy(saveError = null) }
            }
        }
    }

    private fun persistSave() {
        val current = _state.value
        if (current.isSaving || current.isSaved) return
        _state.update { it.copy(isSaving = true, saveError = null) }
        val now = System.currentTimeMillis()
        val entities = current.questions.map { q ->
            InterviewResponseEntity(
                responseId = "${sessionId}_${q.id}",
                participantId = participantId,
                sessionId = sessionId,
                questionId = q.id,
                questionNumber = q.number,
                questionPrompt = q.prompt,
                evaluatorNotes = current.notes[q.id].orEmpty(),
                timestamp = now
            )
        }
        viewModelScope.launch {
            try {
                val dao = AppContainer.database.interviewDao()
                // 1. Persist responses
                dao.insertAllResponses(entities)
                // 2. Persist status (must be done before marking session complete)
                dao.insertInterviewStatus(
                    InterviewStatusEntity(
                        participantId = participantId,
                        sessionId = sessionId,
                        status = "SAVED",
                        timestamp = now
                    )
                )
                _state.update { it.copy(isSaving = false, isSaved = true) }
                onFinished()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Error al guardar. Inténtelo de nuevo."
                    )
                }
            }
        }
    }

    private fun persistSkip() {
        val current = _state.value
        if (current.isSaving || current.isSaved) return
        _state.update { it.copy(isSaving = true, saveError = null) }
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val dao = AppContainer.database.interviewDao()
                // Persist skipped status before invoking onFinished / session completion
                dao.insertInterviewStatus(
                    InterviewStatusEntity(
                        participantId = participantId,
                        sessionId = sessionId,
                        status = "SKIPPED",
                        timestamp = now
                    )
                )
                _state.update { it.copy(isSaving = false, isSaved = true) }
                onFinished()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Error al omitir. Inténtelo de nuevo."
                    )
                }
            }
        }
    }
}
