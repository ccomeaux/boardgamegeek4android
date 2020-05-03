package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdExpansionsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamesExpansions.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_EXPANSIONS"

    override val defaultSortOrder = GamesExpansions.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_EXPANSIONS_JOIN_EXPANSIONS)
                .mapToTable(_ID, Tables.GAMES_EXPANSIONS)
                .mapToTable(GAME_ID, Tables.GAMES_EXPANSIONS)
                .whereEquals("${Tables.GAMES_EXPANSIONS}.$GAME_ID", gameId)
    }

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_EXPANSIONS).whereEquals(GAME_ID, gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_EXPANSIONS, null, values)
        return Games.buildExpansionUri(rowId)
    }
}
