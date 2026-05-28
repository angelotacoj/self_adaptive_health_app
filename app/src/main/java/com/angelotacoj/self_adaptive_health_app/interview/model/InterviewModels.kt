package com.angelotacoj.self_adaptive_health_app.interview.model

/**
 * Phase C1.5 – Short semi-structured interview questions.
 *
 * These are asked by the evaluator/researcher after both conditions
 * and both UEQ questionnaires are complete.
 *
 * Source: paper-2 qualitative interview protocol (Spanish).
 */
data class InterviewQuestion(
    val id: String,         // e.g. "IQ01"
    val number: Int,        // 1-based display order
    val prompt: String      // Spanish question text shown to evaluator
)

/** The official fixed question set for Phase C1.5. */
val INTERVIEW_QUESTIONS: List<InterviewQuestion> = listOf(
    InterviewQuestion(
        id = "IQ01", number = 1,
        prompt = "¿Cuál de las dos interfaces le resultó más fácil de usar?"
    ),
    InterviewQuestion(
        id = "IQ02", number = 2,
        prompt = "¿Las adaptaciones le parecieron útiles?"
    ),
    InterviewQuestion(
        id = "IQ03", number = 3,
        prompt = "¿Sintió que mantenía el control de la aplicación?"
    ),
    InterviewQuestion(
        id = "IQ04", number = 4,
        prompt = "¿Le resultó claro que podía aceptar, rechazar o deshacer cambios?"
    ),
    InterviewQuestion(
        id = "IQ05", number = 5,
        prompt = "¿Qué mejoraría de la aplicación?"
    )
)
