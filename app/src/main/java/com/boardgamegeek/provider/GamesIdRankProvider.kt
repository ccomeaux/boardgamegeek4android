package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdRankProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameRanks.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_RANKS"

    override val defaultSortOrder = GameRanks.DEFAULT_SORT

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
                .table(Tables.GAME_RANKS)
                .whereEquals("${Tables.GAME_RANKS}.$GAME_ID", Games.getGameId(uri))
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
                .table(Tables.GAMES_RANKS_JOIN_GAMES)
                .whereEquals("${Tables.GAME_RANKS}.$GAME_ID", Games.getGameId(uri))
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GameRanks.GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAME_RANKS, null, values)
        return GameRanks.buildGameRankUri(rowId.toInt())
    }
}
