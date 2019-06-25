package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.events.PlaysCountChangedEvent
import com.boardgamegeek.extensions.setActionBarCount
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.startActivity

class PlayerPlaysActivity : SimpleSinglePaneActivity() {
    private var name = ""
    private var playCount = -1

    override val optionsMenuId: Int
        get() = R.menu.player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (name.isNotBlank()) {
            supportActionBar?.subtitle = name
        }

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("PlayerPlays")
                    .putContentName(name))
        }
    }

    override fun readIntent(intent: Intent) {
        name = intent.getStringExtra(KEY_PLAYER_NAME) ?: ""
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlaysFragment.newInstanceForPlayer(name)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_list_count, playCount)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                BuddyActivity.startUp(this, "", name)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(sticky = true)
    fun onEvent(event: PlaysCountChangedEvent) {
        playCount = event.count
        invalidateOptionsMenu()
    }

    companion object {
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, playerName: String?) {
            context.startActivity<PlayerPlaysActivity>(
                    KEY_PLAYER_NAME to playerName
            )
        }
    }
}
