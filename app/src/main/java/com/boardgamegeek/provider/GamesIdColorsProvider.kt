package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

/**
 *  /games/13/colors
 */
class GamesIdColorsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameColors.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_COLORS"

    override val defaultSortOrder = GameColors.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.GAME_COLORS)
                .whereEquals(GAME_ID, gameId)
    }

    /**
     * @return /games/13/colors/green
     */
    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val gameId = Games.getGameId(uri)
        values.put(GameColors.GAME_ID, gameId)
        val rowId = db.insertOrThrow(Tables.GAME_COLORS, null, values)
        return if (rowId != -1L) {
            Games.buildColorsUri(gameId, values.getAsString(GameColors.COLOR))
        } else null
    }
}
