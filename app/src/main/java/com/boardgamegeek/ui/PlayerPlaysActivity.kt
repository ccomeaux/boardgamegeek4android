package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import org.jetbrains.anko.startActivity

class PlayerPlaysActivity : SimpleSinglePaneActivity() {
    private val viewModel by lazy {
        ViewModelProviders.of(this).get(PlaysViewModel::class.java)
    }

    private var name = ""
    private var playCount = -1

    override val optionsMenuId: Int
        get() = R.menu.player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (name.isNotBlank()) {
            supportActionBar?.subtitle = name
        }

        viewModel.setPlayerName(name)
        viewModel.plays.observe(this, Observer {
            playCount = it.data?.sumBy { play -> play.quantity } ?: 0
            invalidateOptionsMenu()
        })
    }

    override fun readIntent(intent: Intent) {
        name = intent.getStringExtra(KEY_PLAYER_NAME) ?: ""
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlaysFragment.newInstanceForPlayer()
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

    companion object {
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, playerName: String?) {
            context.startActivity<PlayerPlaysActivity>(
                    KEY_PLAYER_NAME to playerName
            )
        }
    }
}
