package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent

class AddFieldEvent : CustomEvent("AddField") {
    companion object {
        @JvmStatic
        fun log(contentType: String, sortBy: String) {
            val event = AddFieldEvent().apply {
                putCustomAttribute("contentType", contentType)
                putCustomAttribute("SortBy", sortBy)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
