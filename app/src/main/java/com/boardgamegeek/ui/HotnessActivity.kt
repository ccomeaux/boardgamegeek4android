package com.boardgamegeek.ui

import android.view.MenuItem
import com.boardgamegeek.R
import org.jetbrains.anko.intentFor


class HotnessActivity : TopLevelSinglePaneActivity() {
    override val answersContentType = "Hotness"

    override fun onCreatePane() = HotnessFragment()

    override val navigationItemId = R.id.hotness

    override val optionsMenuId = R.menu.search

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                startActivity(intentFor<SearchResultsActivity>())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
