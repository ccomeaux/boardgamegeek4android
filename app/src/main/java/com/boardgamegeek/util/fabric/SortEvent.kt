package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent

class SortEvent : CustomEvent("Sort") {
    companion object {
        @JvmStatic
        fun log(contentType: String, sortBy: String) {
            val event = SortEvent().apply {
                putCustomAttribute("contentType", contentType)
                putCustomAttribute("SortBy", sortBy)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
