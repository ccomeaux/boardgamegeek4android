package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.dialog.EditTextDialogFragment
import org.jetbrains.anko.startActivity

class GameColorsActivity : SimpleSinglePaneActivity(), EditTextDialogFragment.EditTextDialogListener {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    @ColorInt
    private var iconColor: Int = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (gameName.isNotBlank()) {
            supportActionBar?.subtitle = gameName
        }
    }

    override fun readIntent(intent: Intent) {
        gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME) ?: ""
        iconColor = intent.getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT)
    }

    override fun onCreatePane(intent: Intent): Fragment {
        return ColorsFragment.newInstance(gameId, iconColor)
    }

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

    override fun onFinishEditDialog(text: String, originalText: String?) {
        if (text.isNotBlank()) {
            (fragment as ColorsFragment).addColor(text)
        }
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
