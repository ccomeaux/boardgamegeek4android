package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers

class PlayerColorsManipulationEvent(action: String) : DataManipulationEvent("PlayerColors") {

    init {
        putCustomAttribute("action", action)
    }

    companion object {
        fun log(action: String) {
            val event = PlayerColorsManipulationEvent(action)
            Answers.getInstance().logCustom(event)
        }

        fun log(action: String, color: String) {
            val event = PlayerColorsManipulationEvent(action).apply {
                putCustomAttribute("Color", color)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
