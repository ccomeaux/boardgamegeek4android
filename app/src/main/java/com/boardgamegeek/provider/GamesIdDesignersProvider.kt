package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesDesigners.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdDesignersProvider : BaseProvider() {
    override fun getType(uri: Uri) = Designers.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_DESIGNERS"

    override val defaultSortOrder = Designers.DEFAULT_SORT

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_DESIGNERS).whereEquals(GAME_ID, gameId)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_DESIGNERS_JOIN_DESIGNERS)
                .mapToTable(Designers._ID, Tables.DESIGNERS)
                .mapToTable(Designers.DESIGNER_ID, Tables.DESIGNERS)
                .mapToTable(SyncColumns.UPDATED, Tables.DESIGNERS)
                .whereEquals("${Tables.GAMES_DESIGNERS}.$GAME_ID", gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_DESIGNERS, null, values)
        return Games.buildDesignersUri(rowId)
    }
}
