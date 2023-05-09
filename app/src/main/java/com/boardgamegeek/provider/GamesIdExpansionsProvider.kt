package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.Companion.PATH_EXPANSIONS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.GamesExpansions
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdExpansionsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamesExpansions.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_EXPANSIONS"

    override val defaultSortOrder = GamesExpansions.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
            .table(Tables.GAMES_EXPANSIONS_JOIN_GAMES)
            .mapToTable(_ID, Tables.GAMES_EXPANSIONS)
            .mapToTable(GamesExpansions.Columns.GAME_ID, Tables.GAMES_EXPANSIONS)
            .whereEquals("${Tables.GAMES_EXPANSIONS}.${GamesExpansions.Columns.GAME_ID}", gameId)
    }

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_EXPANSIONS).whereEquals(GamesExpansions.Columns.GAME_ID, gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GamesExpansions.Columns.GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_EXPANSIONS, null, values)
        return Games.buildExpansionUri(rowId)
    }
}
