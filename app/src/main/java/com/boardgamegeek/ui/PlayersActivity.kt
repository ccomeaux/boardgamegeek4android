package com.boardgamegeek.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.events.PlayersCountChangedEvent
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.sorter.PlayersSorterFactory
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.greenrobot.eventbus.Subscribe

class PlayersActivity : SimpleSinglePaneActivity() {
    private var playerCount = -1

    override val optionsMenuId: Int
        get() = R.menu.players

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent().putContentType("Players"))
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlayersFragment()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when ((fragment as? PlayersFragment)?.sortType) {
            PlayersSorterFactory.TYPE_QUANTITY -> menu.findItem(R.id.menu_sort_quantity)?.isChecked = true
            PlayersSorterFactory.TYPE_WINS -> menu.findItem(R.id.menu_sort_wins)?.isChecked = true
            PlayersSorterFactory.TYPE_NAME -> menu.findItem(R.id.menu_sort_name)?.isChecked = true
            else -> menu.findItem(R.id.menu_sort_name)?.isChecked = true
        }
        menu.setActionBarCount(R.id.menu_list_count, playerCount)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                (fragment as PlayersFragment).sortType = PlayersSorterFactory.TYPE_NAME
                return true
            }
            R.id.menu_sort_quantity -> {
                (fragment as PlayersFragment).sortType = PlayersSorterFactory.TYPE_QUANTITY
                return true
            }
            R.id.menu_sort_wins -> {
                (fragment as PlayersFragment).sortType = PlayersSorterFactory.TYPE_WINS
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getDrawerResId() = R.string.title_players

    @Subscribe(sticky = true)
    fun onEvent(event: PlayersCountChangedEvent) {
        playerCount = event.count
        invalidateOptionsMenu()
    }
}
