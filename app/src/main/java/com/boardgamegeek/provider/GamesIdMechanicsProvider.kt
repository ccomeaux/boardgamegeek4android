package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggDatabase.GamesMechanics.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdMechanicsProvider : BaseProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_MECHANICS"

    override val defaultSortOrder = Mechanics.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
            .table(Tables.GAMES_MECHANICS_JOIN_MECHANICS)
            .mapToTable(BaseColumns._ID, Tables.MECHANICS)
            .mapToTable(Mechanics.Columns.MECHANIC_ID, Tables.MECHANICS)
            .mapToTable(Mechanics.Columns.UPDATED, Tables.MECHANICS)
            .whereEquals("${Tables.GAMES_MECHANICS}.$GAME_ID", gameId)
    }

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_MECHANICS).whereEquals(GAME_ID, gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_MECHANICS, null, values)
        return Games.buildMechanicUri(rowId)
    }
}
