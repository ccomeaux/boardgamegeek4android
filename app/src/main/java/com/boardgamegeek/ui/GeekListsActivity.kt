package com.boardgamegeek.ui

import android.support.v4.app.Fragment
import com.boardgamegeek.R

class GeekListsActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "GeekLists"

    override fun onCreatePane(): Fragment = GeekListsFragment()

    override fun getDrawerResId() = R.string.title_geeklists
}
