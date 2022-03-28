package com.boardgamegeek.ui

import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.ui.viewmodel.PlaysSummaryViewModel

class PlaysSummaryActivity : TopLevelSinglePaneActivity() {
    private val viewModel by viewModels<PlaysSummaryViewModel>()

    override val optionsMenuId = R.menu.plays_summary

    override val firebaseContentType = "PlaysSummary"

    override fun onCreatePane(): Fragment = PlaysSummaryFragment()

    override val navigationItemId = R.id.plays

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.re_sync) {
            createThemedBuilder()
                .setTitle(getString(R.string.pref_sync_re_sync_plays) + "?")
                .setMessage(R.string.pref_sync_re_sync_plays_info_message)
                .setPositiveButton(R.string.re_sync) { _, _ -> viewModel.reset() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .show()
            true
        } else super.onOptionsItemSelected(item)
    }
}
