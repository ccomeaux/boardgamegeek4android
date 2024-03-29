package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayersActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlayersViewModel>()

    private var playerCount = -1
    private var sortType = PlayersViewModel.SortType.NAME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Players")
            }
        }

        viewModel.sort(intent.extras?.getSerializableCompat(KEY_SORT_TYPE) ?: PlayersViewModel.SortType.NAME)
        viewModel.players.observe(this) {
            playerCount = it?.size ?: 0
            invalidateOptionsMenu()
        }
        viewModel.sort.observe(this) {
            sortType = it?.sortType ?: PlayersViewModel.SortType.NAME
            invalidateOptionsMenu()
        }
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return PlayersFragment()
    }

    override val optionsMenuId = R.menu.players

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        when (sortType) {
            PlayersViewModel.SortType.NAME -> menu.findItem(R.id.menu_sort_name)
            PlayersViewModel.SortType.PLAY_COUNT -> menu.findItem(R.id.menu_sort_quantity)
            PlayersViewModel.SortType.WIN_COUNT -> menu.findItem(R.id.menu_sort_wins)
        }.apply {
            isChecked = true
            menu.setActionBarCount(R.id.menu_list_count, playerCount, title.toString())
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> viewModel.sort(PlayersViewModel.SortType.NAME)
            R.id.menu_sort_quantity -> viewModel.sort(PlayersViewModel.SortType.PLAY_COUNT)
            R.id.menu_sort_wins -> viewModel.sort(PlayersViewModel.SortType.WIN_COUNT)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {
        private const val KEY_SORT_TYPE = "SORT_TYPE"

        fun start(context: Context) {
            context.startActivity<PlayersActivity>()
        }

        fun startByPlayCount(context: Context) {
            context.startActivity<PlayersActivity>(
                KEY_SORT_TYPE to PlayersViewModel.SortType.PLAY_COUNT
            )
        }
    }
}
