package com.boardgamegeek.util.shortcut

import android.content.Context
import android.content.Intent
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.ShortcutUtils

class GameShortcutTask(context: Context?, private val gameId: Int, private val gameName: String, thumbnailUrl: String?)
    : ShortcutTask(context, thumbnailUrl) {
    override val shortcutName = gameName

    override fun createIntent(): Intent? {
        return context?.let { GameActivity.createIntentAsShortcut(it, gameId, gameName, thumbnailUrl) }
    }

    override val id = ShortcutUtils.createGameShortcutId(gameId)
}
