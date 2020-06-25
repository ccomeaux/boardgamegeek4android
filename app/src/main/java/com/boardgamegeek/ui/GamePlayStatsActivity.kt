package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity.Companion.startUp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.startActivity

class GamePlayStatsActivity : SimpleSinglePaneActivity() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    @ColorInt
    private var headerColor = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (gameName.isNotBlank()) {
            supportActionBar?.subtitle = gameName
        }
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GamePlayStats")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }
    }

    override fun readIntent(intent: Intent) {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME)
        headerColor = intent.getIntExtra(KEY_HEADER_COLOR, ContextCompat.getColor(this, R.color.accent))
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return GamePlayStatsFragment.newInstance(gameId, headerColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startUp(this, gameId, gameName)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HEADER_COLOR = "HEADER_COLOR"

        fun start(context: Context, gameId: Int, gameName: String, @ColorInt headerColor: Int) {
            context.startActivity<GamePlayStatsActivity>(
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_HEADER_COLOR to headerColor
            )
        }
    }
}