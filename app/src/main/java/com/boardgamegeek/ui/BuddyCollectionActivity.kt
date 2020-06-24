package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.ui.BuddyActivity.Companion.startUp
import com.boardgamegeek.ui.viewmodel.BuddyCollectionViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.startActivity
import timber.log.Timber

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

        val statusEntries = resources.getStringArray(com.boardgamegeek.R.array.pref_sync_status_entries)
        val statusValues = resources.getStringArray(com.boardgamegeek.R.array.pref_sync_status_values)
        val statuses = statusValues.zip(statusEntries).toMap()

        viewModel.setUsername(buddyName)
        viewModel.status.observe(this, Observer {
            val status = statuses[it ?: ""]
            supportActionBar?.subtitle = buddyName + if (status != null && status.isNotEmpty()) {
                " - $status"
            } else {
                ""
            }
            invalidateOptionsMenu()
        })
    }

    override fun readIntent(intent: Intent) {
        buddyName = intent.getStringExtra(KEY_BUDDY_NAME)
    }

    override fun onCreatePane(intent: Intent) = BuddyCollectionFragment()

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