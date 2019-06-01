package com.boardgamegeek.ui

import androidx.fragment.app.Fragment
import com.boardgamegeek.R

class GeekListsActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "GeekLists"

    override fun onCreatePane(): Fragment = GeekListsFragment()

    override val navigationItemId = R.id.geeklists
}
