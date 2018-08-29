package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent

class PlayStatsActivity : SimpleSinglePaneActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent().putContentType("PlayStats"))
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlayStatsFragment()
    }
}
