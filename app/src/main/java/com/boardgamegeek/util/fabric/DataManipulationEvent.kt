package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent

open class DataManipulationEvent protected constructor(contentType: String) : CustomEvent("DataManipulation") {
    init {
        putCustomAttribute("contentType", contentType)
    }

    companion object {
        fun log(contentType: String, action: String) {
            val event = DataManipulationEvent(contentType).apply {
                putCustomAttribute("Action", action)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
