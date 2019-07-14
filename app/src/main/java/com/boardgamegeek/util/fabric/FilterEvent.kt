package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent

class FilterEvent : CustomEvent("Filter") {
    companion object {
        @JvmStatic
        fun log(contentType: String, filterBy: String) {
            val event = FilterEvent().apply {
                putCustomAttribute("contentType", contentType)
                putCustomAttribute("FilterBy", filterBy)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
