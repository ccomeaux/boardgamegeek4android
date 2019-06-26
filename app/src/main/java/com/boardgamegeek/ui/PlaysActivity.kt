package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.events.PlaysCountChangedEvent
import com.boardgamegeek.events.PlaysFilterChangedEvent
import com.boardgamegeek.events.PlaysSortChangedEvent
import com.boardgamegeek.extensions.setActionBarCount
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PlaysActivity : SimpleSinglePaneActivity() {
    private var playCount: Int = 0
    private var sortName: String? = null

    override val optionsMenuId: Int
        get() = R.menu.plays

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent().putContentType("Plays"))
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlaysFragment.newInstance()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_list_count, playCount, getString(R.string.by_prefix, sortName))
        return true
    }

    @Subscribe(sticky = true)
    fun onEvent(event: PlaysCountChangedEvent) {
        playCount = event.count
        invalidateOptionsMenu()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: PlaysFilterChangedEvent) {
        supportActionBar?.subtitle = if (event.type == PlaysFragment.FILTER_TYPE_STATUS_ALL) "" else event.description
    }

    @Subscribe(sticky = true)
    fun onEvent(event: PlaysSortChangedEvent) {
        sortName = event.description
        invalidateOptionsMenu()
    }
}
