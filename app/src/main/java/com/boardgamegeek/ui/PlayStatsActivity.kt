package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class PlayStatsActivity : SimpleSinglePaneActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayStats")
            }
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlayStatsFragment()
    }
}
