package com.boardgamegeek.ui

import androidx.fragment.app.Fragment

import com.boardgamegeek.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncActivity : TopLevelSinglePaneActivity() {
    override val firebaseContentType = "Sync"

    override fun onCreatePane(): Fragment = SyncFragment()

    override val navigationItemId: Int = R.id.sync
}
