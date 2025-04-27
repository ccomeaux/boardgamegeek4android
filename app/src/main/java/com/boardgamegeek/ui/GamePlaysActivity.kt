package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import com.boardgamegeek.R
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GamePlaysActivity : SimpleSinglePaneActivity() {
    private val viewModel by viewModels<PlaysViewModel>()

    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var heroImageUrl = ""
    private var thumbnailUrl = ""
    private var arePlayersCustomSorted = false

    @ColorInt
    private var iconColor = Color.TRANSPARENT
    private var playCount = -1

    override val optionsMenuId: Int
        get() = R.menu.text_only

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (gameName.isNotBlank()) {
            supportActionBar?.subtitle = gameName
        }

        viewModel.setGame(gameId)
        viewModel.plays.observe(this) {
            playCount = it?.sumOf { play -> play.quantity } ?: 0
            invalidateOptionsMenu()
        }
    }

    override fun readIntent() {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        arePlayersCustomSorted = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)
        iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT)
    }

    override fun createPane() = PlaysFragment.newInstanceForGame(gameId, gameName, heroImageUrl, arePlayersCustomSorted, iconColor)

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.setActionBarCount(R.id.menu_text, playCount)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                GameActivity.startUp(this, gameId, gameName, thumbnailUrl, heroImageUrl)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_ICON_COLOR = "ICON_COLOR"

        fun start(
            context: Context,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            thumbnailUrl: String,
            arePlayersCustomSorted: Boolean,
            @ColorInt iconColor: Int
        ) {
            context.startActivity(createIntent(context, gameId, gameName, heroImageUrl, thumbnailUrl, arePlayersCustomSorted, iconColor))
        }

        fun createIntent(
            context: Context,
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            thumbnailUrl: String = heroImageUrl,
            arePlayersCustomSorted: Boolean = false,
            @ColorInt iconColor: Int = Color.TRANSPARENT,
        ): Intent {
            return context.intentFor<GamePlaysActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_HERO_IMAGE_URL to heroImageUrl,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_CUSTOM_PLAYER_SORT to arePlayersCustomSorted,
                KEY_ICON_COLOR to iconColor,
            )
        }
    }
}
