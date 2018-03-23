package com.boardgamegeek.ui

import android.support.v4.app.Fragment
import android.view.Menu
import com.boardgamegeek.R
import com.boardgamegeek.events.BuddiesCountChangedEvent
import com.boardgamegeek.events.BuddySelectedEvent
import com.boardgamegeek.util.ToolbarUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.ctx

class BuddiesActivity : TopLevelSinglePaneActivity() {
    private var numberOfBuddies = -1

    override val answersContentType = "Buddies"

    override val optionsMenuId = R.menu.buddies

    override fun onCreatePane(): Fragment = BuddiesFragment()

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, if (numberOfBuddies <= 0) "" else String.format("%,d", numberOfBuddies))
        return super.onPrepareOptionsMenu(menu)
    }

    override fun getDrawerResId() = R.string.title_buddies

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(event: BuddiesCountChangedEvent) {
        numberOfBuddies = event.count
        invalidateOptionsMenu()
    }

    @Subscribe
    fun onEvent(event: BuddySelectedEvent) = BuddyActivity.start(ctx, event.buddyName, event.buddyFullName)
}
