package com.boardgamegeek.ui

import android.os.Bundle
import com.boardgamegeek.R
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent

class HotnessActivity : TopLevelSinglePaneActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent().putContentType("Hotness"))
        }
    }

    override fun onCreatePane() = HotnessFragment()

    override fun getDrawerResId() = R.string.title_hotness
}
