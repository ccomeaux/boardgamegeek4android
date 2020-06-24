package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameCommentsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.startActivity

class CommentsActivity : SimpleSinglePaneActivity() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var sortType = SORT_TYPE_USER
    private val viewModel by viewModels<GameCommentsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setGameId(gameId)
        viewModel.setSort(if (sortType == SORT_TYPE_USER) GameCommentsViewModel.SortType.USER else GameCommentsViewModel.SortType.RATING)
        viewModel.sort.observe(this, Observer {
            sortType = if (it == GameCommentsViewModel.SortType.RATING) SORT_TYPE_RATING else SORT_TYPE_USER
            invalidateOptionsMenu()
        })

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameComments")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME) ?: ""
        sortType = intent.getIntExtra(KEY_SORT_TYPE, SORT_TYPE_USER)
    }

    override fun onCreatePane(intent: Intent) = CommentsFragment()

    override val optionsMenuId = R.menu.game_comments

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (gameName.isNotEmpty()) {
            supportActionBar?.title = gameName
            supportActionBar?.setSubtitle(if (sortType == SORT_TYPE_RATING) R.string.title_ratings else R.string.title_comments)
        } else {
            supportActionBar?.setTitle(if (sortType == SORT_TYPE_RATING) R.string.title_ratings else R.string.title_comments)
        }

        if (sortType == SORT_TYPE_RATING) {
            menu.findItem(R.id.menu_sort_rating)?.isChecked = true
        } else {
            menu.findItem(R.id.menu_sort_comments)?.isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                GameActivity.startUp(this, gameId, gameName)
                finish()
                return true
            }
            R.id.menu_sort_comments -> {
                sortType = SORT_TYPE_USER
                invalidateOptionsMenu()
                (fragment as? CommentsFragment)?.clear()
                viewModel.setSort(GameCommentsViewModel.SortType.USER)
                return true
            }
            R.id.menu_sort_rating -> {
                sortType = SORT_TYPE_RATING
                invalidateOptionsMenu()
                (fragment as? CommentsFragment)?.clear()
                viewModel.setSort(GameCommentsViewModel.SortType.RATING)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_SORT_TYPE = "SORT_TYPE"
        const val SORT_TYPE_USER = 0
        const val SORT_TYPE_RATING = 1

        fun startRating(context: Context, gameId: Int, gameName: String) {
            context.startActivity<CommentsActivity>(
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_SORT_TYPE to SORT_TYPE_RATING
            )
        }
    }
}