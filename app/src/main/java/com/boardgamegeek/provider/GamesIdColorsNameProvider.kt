package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GameColorsColumns.COLOR
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

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
                .whereEquals(GAME_ID, gameId)
                .whereEquals(COLOR, color)
    }
}
