package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLORS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggDatabase.Tables

/**
 * /games/13/colors/green
 */
class GamesIdColorsNameProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameColors.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_COLORS/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val color = uri.lastPathSegment
        return SelectionBuilder()
            .table(Tables.GAME_COLORS)
            .whereEquals(GameColors.Columns.GAME_ID, gameId)
            .whereEquals(GameColors.Columns.COLOR, color)
    }
}
