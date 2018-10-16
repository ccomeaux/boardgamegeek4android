package com.boardgamegeek.ui

import android.view.Menu
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.events.BuddiesCountChangedEvent
import com.boardgamegeek.extensions.setActionBarCount
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class BuddiesActivity : TopLevelSinglePaneActivity() {
    private var numberOfBuddies = -1

    override val answersContentType = "Buddies"

    override val optionsMenuId = R.menu.buddies

    override fun onCreatePane(): Fragment = BuddiesFragment()

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.setActionBarCount(R.id.menu_list_count, numberOfBuddies)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun getDrawerResId() = R.string.title_buddies

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: BuddiesCountChangedEvent) {
        numberOfBuddies = event.count
        invalidateOptionsMenu()
    }
}
