package com.boardgamegeek.ui

import android.support.v4.app.Fragment
import android.view.MenuItem
import com.boardgamegeek.R
import com.boardgamegeek.events.PlaySelectedEvent
import com.boardgamegeek.tasks.ResetPlaysTask
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.TaskUtils
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.ctx

class PlaysSummaryActivity : TopLevelSinglePaneActivity() {

    override val optionsMenuId = R.menu.plays_summary

    override val answersContentType = "PlaysSummary"

    override fun onCreatePane(): Fragment = PlaysSummaryFragment()

    override fun getDrawerResId() = R.string.title_plays

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.re_sync -> {
                DialogUtils.createThemedBuilder(ctx)
                        .setTitle(getString(R.string.pref_sync_re_sync_plays) + "?")
                        .setMessage(R.string.pref_sync_re_sync_plays_info_message)
                        .setPositiveButton(R.string.re_sync) { _, _ -> TaskUtils.executeAsyncTask(ResetPlaysTask(ctx)) }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Subscribe
    fun onEvent(event: PlaySelectedEvent) = PlayActivity.start(ctx, event)
}
