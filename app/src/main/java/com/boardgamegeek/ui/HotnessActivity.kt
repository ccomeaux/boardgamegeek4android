package com.boardgamegeek.ui

import android.os.Bundle
import android.view.MenuItem
import com.boardgamegeek.R
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.jetbrains.anko.intentFor


class HotnessActivity : TopLevelSinglePaneActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent().putContentType("Hotness"))
        }
    }

    override fun onCreatePane() = HotnessFragment()

    override fun getDrawerResId() = R.string.title_hotness

    override val optionsMenuId = R.menu.search

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_search) {
            startActivity(intentFor<SearchResultsActivity>())
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
