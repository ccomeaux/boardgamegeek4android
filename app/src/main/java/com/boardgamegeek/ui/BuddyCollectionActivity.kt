package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import com.boardgamegeek.extensions.createStatusMap
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.BuddyActivity.Companion.startUp
import com.boardgamegeek.ui.viewmodel.BuddyCollectionViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class BuddyCollectionActivity : SimpleSinglePaneActivity() {
    private var buddyName = ""
    val viewModel by viewModels<BuddyCollectionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (buddyName.isBlank()) {
            Timber.w("Missing buddy name.")
            finish()
        }

        supportActionBar?.subtitle = buddyName

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName)
            }
        }

        val statuses = this.createStatusMap()

        viewModel.setUsername(buddyName)
        viewModel.status.observe(this) {
            val status = statuses[it.orEmpty()]
            supportActionBar?.subtitle = buddyName + (if (!status.isNullOrEmpty()) " - $status" else "")
            invalidateOptionsMenu()
        }
    }

    override fun readIntent() {
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME).orEmpty()
    }

    override fun createPane() = BuddyCollectionFragment()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startUp(this, buddyName)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"

        fun start(context: Context, buddyName: String?) {
            context.startActivity<BuddyCollectionActivity>(KEY_BUDDY_NAME to buddyName)
        }
    }
}
