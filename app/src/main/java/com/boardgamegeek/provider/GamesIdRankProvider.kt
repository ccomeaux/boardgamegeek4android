package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_RANKS
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdRankProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameRanks.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_RANKS"

    override val defaultSortOrder = GameRanks.DEFAULT_SORT

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
            .table(Tables.GAME_RANKS)
            .whereEquals("${Tables.GAME_RANKS}.${GameRanks.Columns.GAME_ID}", Games.getGameId(uri))
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
            .table(Tables.GAMES_RANKS_JOIN_GAMES)
            .mapToTable(BaseColumns._ID, Tables.GAME_RANKS)
            .mapToTable(GameRanks.Columns.GAME_ID, Tables.GAME_RANKS)
            .whereEquals("${Tables.GAME_RANKS}.${GameRanks.Columns.GAME_ID}", Games.getGameId(uri))
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(Games.Columns.GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAME_RANKS, null, values)
        return GameRanks.buildGameRankUri(rowId.toInt())
    }
}
