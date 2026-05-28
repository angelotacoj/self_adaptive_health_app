package com.angelotacoj.self_adaptive_health_app.interview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.interview.model.INTERVIEW_QUESTIONS
import com.angelotacoj.self_adaptive_health_app.interview.model.InterviewQuestion
import com.angelotacoj.self_adaptive_health_app.interview.persistence.InterviewResponseEntity
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
    val saveError: String? = null
) {
    val currentQuestion: InterviewQuestion? get() = questions.firstOrNull()
    val answeredCount: Int get() = notes.values.count { it.isNotBlank() }
    val totalQuestions: Int get() = questions.size
}

sealed class InterviewEvent {
    data class NotesChanged(val questionId: String, val text: String) : InterviewEvent()
    object Save : InterviewEvent()
    object Skip : InterviewEvent()
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
     * [onFinished] is invoked on Main after save or skip.
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
            is InterviewEvent.Save -> handleSave()
            is InterviewEvent.Skip -> {
                // Intentional skip: allowed by protocol if evaluator decides not to record notes.
                _state.update { it.copy(isSaved = true) }
                onFinished()
            }
            is InterviewEvent.DismissError -> {
                _state.update { it.copy(saveError = null) }
            }
        }
    }

    private fun handleSave() {
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
                AppContainer.database.interviewDao().insertAllResponses(entities)
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
}
