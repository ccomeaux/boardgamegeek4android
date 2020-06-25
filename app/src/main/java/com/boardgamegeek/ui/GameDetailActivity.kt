package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameViewModel
import com.boardgamegeek.ui.viewmodel.GameViewModel.ProducerType
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.startActivity

class GameDetailActivity : SimpleSinglePaneActivity() {
    private var title: String = ""
    private var gameId: Int = 0
    private var gameName: String = ""
    private var type: ProducerType = ProducerType.UNKNOWN

    private val viewModel by viewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = gameName
        supportActionBar?.subtitle = title

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameDetail$title")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        viewModel.setId(gameId)
        viewModel.setProducerType(type)
    }

    override fun readIntent(intent: Intent) {
        title = intent.getStringExtra(KEY_TITLE)
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME) ?: ""
        type = intent.getSerializableExtra(KEY_TYPE) as ProducerType
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return GameDetailFragment()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                when (gameId) {
                    BggContract.INVALID_ID -> onBackPressed()
                    else -> GameActivity.startUp(this, gameId, gameName)
                }
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_TITLE = "TITLE"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_TYPE = "TYPE"

        fun start(context: Context, title: String, gameId: Int, gameName: String, type: ProducerType) {
            context.startActivity<GameDetailActivity>(
                    KEY_TITLE to title,
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_TYPE to type)
        }
    }
}
