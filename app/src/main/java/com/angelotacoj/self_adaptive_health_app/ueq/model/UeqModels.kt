package com.angelotacoj.self_adaptive_health_app.ueq.model

import com.angelotacoj.self_adaptive_health_app.adaptive.domain.model.ExperimentCondition
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup

/** Questionnaire mode. Only the full 26-item Spanish UEQ is used in this experiment. */
enum class UeqMode {
    UEQ_FULL_26
}

/**
 * One item in the official Spanish UEQ item bank.
 * Items are ordered exactly as in the official UEQ_Spanish.pdf.
 *
 * [leftLabel] is the left (negative) pole; [rightLabel] is the right (positive) pole.
 * Scale runs 1 (strongly left) to 7 (strongly right).
 */
data class UeqItem(
    val id: String,          // e.g. "UEQ01"
    val number: Int,         // 1-26
    val leftLabel: String,
    val rightLabel: String
)

/**
 * Response for a single UEQ item, fully linked to session metadata so it can be
 * exported and cross-referenced with adaptation logs.
 *
 * [selectedValue] is 1..7 (1 = strongly leftLabel, 7 = strongly rightLabel).
 */
data class UeqItemResponse(
    val participantId: String,
    val sessionId: String,
    val group: ExperimentGroup,
    val condition: ExperimentCondition,
    val questionnaireMode: UeqMode = UeqMode.UEQ_FULL_26,
    val timestamp: Long,
    val itemId: String,
    val itemNumber: Int,
    val leftLabel: String,
    val rightLabel: String,
    val selectedValue: Int   // 1..7
)

/**
 * Complete UEQ response set for one condition (26 item responses).
 */
data class UeqResponse(
    val participantId: String,
    val sessionId: String,
    val group: ExperimentGroup,
    val condition: ExperimentCondition,
    val questionnaireMode: UeqMode = UeqMode.UEQ_FULL_26,
    val completedAt: Long,
    val items: List<UeqItemResponse>
)

// ---------------------------------------------------------------------------
// Official Spanish UEQ Item Bank (UEQ_Spanish.pdf – 26 items, exact order)
// ---------------------------------------------------------------------------

val UEQ_FULL_26_ITEMS: List<UeqItem> = listOf(
    UeqItem("UEQ01",  1,  "desagradable",            "agradable"),
    UeqItem("UEQ02",  2,  "no entendible",            "entendible"),
    UeqItem("UEQ03",  3,  "creativo",                 "sin imaginación"),
    UeqItem("UEQ04",  4,  "fácil de aprender",        "difícil de aprender"),
    UeqItem("UEQ05",  5,  "valioso",                  "de poco valor"),
    UeqItem("UEQ06",  6,  "aburrido",                 "emocionante"),
    UeqItem("UEQ07",  7,  "no interesante",           "interesante"),
    UeqItem("UEQ08",  8,  "impredecible",             "predecible"),
    UeqItem("UEQ09",  9,  "rápido",                   "lento"),
    UeqItem("UEQ10", 10,  "original",                 "convencional"),
    UeqItem("UEQ11", 11,  "obstructivo",              "impulsor de apoyo"),
    UeqItem("UEQ12", 12,  "bueno",                    "malo"),
    UeqItem("UEQ13", 13,  "complicado",               "fácil"),
    UeqItem("UEQ14", 14,  "repeler",                  "atraer"),
    UeqItem("UEQ15", 15,  "convencional",             "novedoso"),
    UeqItem("UEQ16", 16,  "incómodo",                 "cómodo"),
    UeqItem("UEQ17", 17,  "seguro",                   "inseguro"),
    UeqItem("UEQ18", 18,  "activante",                "adormecedor"),
    UeqItem("UEQ19", 19,  "cubre expectativas",       "no cubre expectativas"),
    UeqItem("UEQ20", 20,  "ineficiente",              "eficiente"),
    UeqItem("UEQ21", 21,  "claro",                    "confuso"),
    UeqItem("UEQ22", 22,  "no pragmático",            "pragmático"),
    UeqItem("UEQ23", 23,  "ordenado",                 "sobrecargado"),
    UeqItem("UEQ24", 24,  "atractivo",                "feo"),
    UeqItem("UEQ25", 25,  "simpático",                "antipático"),
    UeqItem("UEQ26", 26,  "conservador",              "innovador")
)
