package com.boardgamegeek.ui

import android.view.MenuItem
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity

class HotnessActivity : TopLevelSinglePaneActivity() {
    override val firebaseContentType = "Hotness"

    override fun onCreatePane() = HotnessFragment()

    override val navigationItemId = R.id.hotness

    override val optionsMenuId = R.menu.search

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> startActivity<SearchResultsActivity>()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
