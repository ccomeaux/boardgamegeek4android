package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers

class PlayManipulationEvent(action: String) : DataManipulationEvent("Play") {

    init {
        putCustomAttribute("action", action)
    }

    companion object {
        @JvmStatic
        fun log(action: String, gameName: String) {
            val event = PlayManipulationEvent(action).apply {
                putCustomAttribute("GameName", gameName)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
