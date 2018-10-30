package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.tasks.AddUsernameToPlayerTask
import com.boardgamegeek.tasks.RenamePlayerTask
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener
import com.boardgamegeek.ui.dialog.EditUsernameDialogFragment
import com.boardgamegeek.ui.dialog.EditUsernameDialogFragment.EditUsernameDialogListener
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import com.boardgamegeek.util.ActivityUtils
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.TaskUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.intentFor
import timber.log.Timber

class BuddyActivity : SimpleSinglePaneActivity(), EditUsernameDialogListener, EditTextDialogListener {
    private var name: String? = null
    private var username: String? = null

    private val viewModel: BuddyViewModel by lazy {
        ViewModelProviders.of(this).get(BuddyViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (name.isNullOrBlank() && username.isNullOrBlank()) finish()
        setSubtitle()

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("Buddy")
                    .putContentId(username)
                    .putContentName(name))
        }

        if (username != null && username?.isNotBlank() == true) {
            viewModel.setUsername(username)
        } else {
            viewModel.setPlayerName(name)
        }

        viewModel.updateMessage.observe(this, Observer {
            showSnackbar(it)
        })
    }

    override fun readIntent(intent: Intent) {
        name = intent.getStringExtra(KEY_PLAYER_NAME)
        username = intent.getStringExtra(KEY_BUDDY_NAME)
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
                ActivityUtils.linkToBgg(this, "user/$username")
                return true
            }
            R.id.add_username -> {
                DialogUtils.showFragment(this, EditUsernameDialogFragment.newInstance(), "add_username")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFinishAddUsername(username: String) {
        if (username.isNotBlank()) {
            val task = AddUsernameToPlayerTask(this, name, username)
            TaskUtils.executeAsyncTask(task)
        }
    }

    override fun onFinishEditDialog(text: String, @Nullable originalText: String?) {
        if (text.isNotBlank()) {
            val task = RenamePlayerTask(this, originalText, text)
            TaskUtils.executeAsyncTask(task)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AddUsernameToPlayerTask.Event) {
        if (event.isSuccessful) {
            username = event.username
            intent.putExtra(KEY_BUDDY_NAME, username)
            setSubtitle()

            recreateFragment()
        }
        showSnackbar(event.message)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(@NonNull event: RenamePlayerTask.Event) {
        name = event.playerName
        intent.putExtra(KEY_PLAYER_NAME, name)
        setSubtitle()
        recreateFragment()
        showSnackbar(event.message)
    }

    private fun setSubtitle() {
        supportActionBar?.subtitle = if (username.isNullOrBlank()) name else username
    }

    private fun showSnackbar(message: String) {
        if (message.isNotBlank()) {
            rootContainer?.longSnackbar(message)
        }
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        @JvmStatic
        fun start(context: Context, username: String, playerName: String) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun startUp(context: Context, username: String, playerName: String? = null) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it.clearTop())
            }
        }

        @JvmStatic
        fun createIntent(context: Context, username: String, playerName: String?): Intent? {
            if (username.isBlank() && playerName.isNullOrBlank()) {
                Timber.w("Unable to create a BuddyActivity intent - missing both a username and a player name")
                return null
            }
            return context.intentFor<BuddyActivity>(
                    KEY_BUDDY_NAME to username,
                    KEY_PLAYER_NAME to playerName
            )
        }
    }
}
