package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment

class PlayStatsActivity : SimpleSinglePaneActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlayStatsFragment()
    }
}
