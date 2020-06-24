package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.startActivity

class BuddyPlaysActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()
    private var buddyName = ""
    private var numberOfPlays = -1

    override val optionsMenuId: Int
        get() = R.menu.text_only

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (buddyName.isNotBlank()) {
            supportActionBar?.subtitle = buddyName
        }
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyPlays")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName)
            }
        }

        viewModel.setUsername(buddyName)
        viewModel.plays.observe(this, Observer {
            numberOfPlays = it.data?.sumBy { play -> play.quantity } ?: 0
            invalidateOptionsMenu()
        })
    }

    override fun readIntent(intent: Intent) {
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME)
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlaysFragment.newInstanceForBuddy()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_text, numberOfPlays)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                BuddyActivity.startUp(this, buddyName)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"

        fun start(context: Context, buddyName: String?) {
            context.startActivity<BuddyPlaysActivity>(
                    KEY_BUDDY_NAME to buddyName
            )
        }
    }
}
