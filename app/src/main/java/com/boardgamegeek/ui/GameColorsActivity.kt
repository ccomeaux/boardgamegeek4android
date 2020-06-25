package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.GameColorsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import hugo.weaving.DebugLog
import org.jetbrains.anko.startActivity

class GameColorsActivity : SimpleSinglePaneActivity() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""

    @ColorInt
    private var iconColor: Int = Color.TRANSPARENT

    private val viewModel by viewModels<GameColorsViewModel>()

    @DebugLog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (gameName.isNotBlank()) {
            supportActionBar?.subtitle = gameName
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameColors")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        viewModel.setGameId(gameId)
    }

    override fun readIntent(intent: Intent) {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME) ?: ""
        iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT)
    }

    @DebugLog
    override fun onCreatePane(intent: Intent): Fragment {
        return GameColorsFragment.newInstance(iconColor)
    }

    @DebugLog
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                GameActivity.startUp(this, gameId, gameName)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        fun start(context: Context, gameId: Int, gameName: String, @ColorInt iconColor: Int) {
            context.startActivity<GameColorsActivity>(
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_ICON_COLOR to iconColor)
        }
    }
}
