package com.angelotacoj.self_adaptive_health_app.ueq.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a single UEQ item response.
 * One row per item, per participant, per condition session.
 *
 * Indexed by sessionId + condition for efficient retrieval during export.
 */
@Entity(
    tableName = "ueq_responses",
    indices = [
        Index(value = ["sessionId", "condition"]),
        Index(value = ["participantId"]),
        Index(value = ["condition"])
    ]
)
data class UeqResponseEntity(
    @PrimaryKey val responseId: String,   // UUID: "<sessionId>_<condition>_<itemId>"
    val participantId: String,
    val sessionId: String,
    val group: String,                    // ExperimentGroup.name
    val condition: String,               // ExperimentCondition.name: "STATIC_UI" | "SELF_ADAPTIVE_UI"
    val questionnaireMode: String,       // UeqMode.name – always "UEQ_FULL_26"
    val timestamp: Long,                 // System.currentTimeMillis() at save time
    val itemId: String,                  // "UEQ01" .. "UEQ26"
    val itemNumber: Int,                 // 1..26
    val leftLabel: String,
    val rightLabel: String,
    val selectedValue: Int               // 1..7
)
