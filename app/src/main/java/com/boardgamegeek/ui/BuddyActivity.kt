package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.dialog.EditUsernameDialogFragment
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import com.google.android.material.snackbar.Snackbar
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.intentFor
import timber.log.Timber

class BuddyActivity : SimpleSinglePaneActivity() {
    private var name: String? = null
    private var username: String? = null
    private var snackbar: Snackbar? = null

    private val viewModel: BuddyViewModel by lazy {
        ViewModelProvider(this).get(BuddyViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (name.isNullOrBlank() && username.isNullOrBlank()) finish()
        setSubtitle()

        if (username != null && username?.isNotBlank() == true) {
            viewModel.setUsername(username)
        } else {
            viewModel.setPlayerName(name)
        }

        viewModel.user.observe(this, Observer {
            when {
                it == null -> return@Observer
                it.second == BuddyViewModel.TYPE_PLAYER && it.first != name -> {
                    name = it.first
                    intent.putExtra(KEY_PLAYER_NAME, name)
                    setSubtitle()
                    recreateFragment()
                }
                it.second == BuddyViewModel.TYPE_USER && it.first != username -> {
                    username = it.first
                    intent.putExtra(KEY_USERNAME, username)
                    setSubtitle()
                    recreateFragment()
                }
            }
        })

        viewModel.updateMessage.observe(this, Observer {
            it.getContentIfNotHandled()?.let { content ->
                showSnackbar(content)
            }
        })
    }

    override fun readIntent(intent: Intent) {
        name = intent.getStringExtra(KEY_PLAYER_NAME)
        username = intent.getStringExtra(KEY_USERNAME)
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return BuddyFragment.newInstance(username, name)
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
                showAndSurvive(EditUsernameDialogFragment.newInstance())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSubtitle() {
        supportActionBar?.subtitle = if (username.isNullOrBlank()) name else username
    }

    private fun showSnackbar(message: String?) {
        if (message == null || message.isBlank()) {
            snackbar?.dismiss()
        } else {
            snackbar = rootContainer?.longSnackbar(message)
        }
    }

    companion object {
        private const val KEY_USERNAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        @JvmStatic
        fun start(context: Context, username: String, playerName: String) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun startUp(context: Context, username: String?, playerName: String? = null) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it.clearTop())
            }
        }

        @JvmStatic
        fun createIntent(context: Context, username: String?, playerName: String?): Intent? {
            if (username.isNullOrBlank() && playerName.isNullOrBlank()) {
                Timber.w("Unable to create a BuddyActivity intent - missing both a username and a player name")
                return null
            }
            return context.intentFor<BuddyActivity>(
                    KEY_USERNAME to username,
                    KEY_PLAYER_NAME to playerName
            )
        }
    }
}
