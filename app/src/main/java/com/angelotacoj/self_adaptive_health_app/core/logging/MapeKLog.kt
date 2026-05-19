package com.angelotacoj.self_adaptive_health_app.core.logging

import android.util.Log

private const val AURA_MAPEK_TAG = "AURA_MAPEK"

object MapeKLog {
    fun stage(stage: String, message: String) {
        Log.d(AURA_MAPEK_TAG, "$stage | $message")
        Log.d("AURA_${stage}", message)
    }

    fun nav(message: String) {
        Log.d("AURA_NAV", message)
    }

    fun experiment(message: String) {
        Log.d("AURA_EXPERIMENT", message)
    }

    fun knowledge(message: String) {
        Log.d("AURA_KNOWLEDGE", message)
    }

    fun state(message: String) {
        Log.d("AURA_STATE", message)
    }
}
