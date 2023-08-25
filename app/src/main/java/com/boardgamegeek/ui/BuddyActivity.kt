package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.dialog.EditUsernameDialogFragment
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class BuddyActivity : SimpleSinglePaneActivity() {
    private var name: String? = null
    private var username: String? = null
    private var snackbar: Snackbar? = null

    private val viewModel by viewModels<BuddyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (name.isNullOrBlank() && username.isNullOrBlank()) finish()
        setSubtitle()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Buddy")
                param(FirebaseAnalytics.Param.ITEM_ID, username.orEmpty())
                param(FirebaseAnalytics.Param.ITEM_NAME, name.orEmpty())
            }
        }

        if (username != null && username?.isNotBlank() == true) {
            viewModel.setUsername(username)
        } else {
            viewModel.setPlayerName(name)
        }

        viewModel.username.observe(this) {
            it?.let {
                username = it
                intent.putExtra(KEY_USERNAME, username)
                setSubtitle()
            }
        }

        viewModel.playerName.observe(this) {
            it?.let {
                name = it
                intent.putExtra(KEY_PLAYER_NAME, name)
                setSubtitle()
            }
        }

        viewModel.updateMessage.observe(this) {
            it.getContentIfNotHandled()?.let { content ->
                showSnackbar(content)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        name = intent.getStringExtra(KEY_PLAYER_NAME)
        username = intent.getStringExtra(KEY_USERNAME)
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return BuddyFragment()
    }

    override val optionsMenuId = R.menu.buddy

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.add_username)?.isVisible = username.isNullOrBlank()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_view -> {
                linkToBgg("user/$username")
                return true
            }
            R.id.add_username -> {
                showAndSurvive(EditUsernameDialogFragment())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSubtitle() {
        supportActionBar?.subtitle = if (username.isNullOrBlank()) name else username
    }

    private fun showSnackbar(message: String?) {
        if (message.isNullOrBlank()) {
            snackbar?.dismiss()
        } else {
            snackbar = rootContainer?.longSnackbar(message)
        }
    }

    companion object {
        private const val KEY_USERNAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, username: String, playerName: String) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it)
            }
        }

        fun startUp(context: Context, username: String?, playerName: String? = null) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it.clearTop())
            }
        }

        fun createIntent(context: Context, username: String?, playerName: String?): Intent? {
            if (username.isNullOrBlank() && playerName.isNullOrBlank()) {
                Timber.w("Unable to create a BuddyActivity intent - missing both a username and a player name")
                return null
            }
            return context.intentFor<BuddyActivity>(
                KEY_USERNAME to username,
                KEY_PLAYER_NAME to playerName,
            )
        }
    }
}
