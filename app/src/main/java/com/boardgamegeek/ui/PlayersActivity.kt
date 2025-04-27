package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayersActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlayersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Players")
            }
        }

        viewModel.sort(intent.extras?.getSerializableCompat(KEY_SORT_TYPE) ?: Player.SortType.NAME)
    }

    override fun createPane() = PlayersFragment()

    companion object {
        private const val KEY_SORT_TYPE = "SORT_TYPE"

        fun start(context: Context) {
            context.startActivity<PlayersActivity>()
        }

        fun startByPlayCount(context: Context) {
            context.startActivity<PlayersActivity>(
                KEY_SORT_TYPE to Player.SortType.PLAY_COUNT
            )
        }
    }
}
