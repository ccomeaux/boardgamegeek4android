package com.boardgamegeek.util.fabric

import com.crashlytics.android.answers.Answers

class CollectionViewManipulationEvent(action: String) : DataManipulationEvent("CollectionView") {

    init {
        putCustomAttribute("action", action)
    }

    companion object {
        @JvmStatic
        fun log(action: String) {
            val event = CollectionViewManipulationEvent(action)
            Answers.getInstance().logCustom(event)
        }

        @JvmStatic
        fun log(action: String, viewName: String) {
            val event = CollectionViewManipulationEvent(action).apply {
                putCustomAttribute("ViewName", viewName)
            }
            Answers.getInstance().logCustom(event)
        }
    }
}
