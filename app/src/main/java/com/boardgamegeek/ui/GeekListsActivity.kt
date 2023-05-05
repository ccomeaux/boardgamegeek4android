package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeekListsActivity : TopLevelSinglePaneActivity() {
    override val firebaseContentType = "GeekLists"

    override fun onCreatePane(): Fragment = GeekListsFragment()

    override val navigationItemId = R.id.geeklists
}
