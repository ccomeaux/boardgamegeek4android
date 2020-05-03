package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesPublishers.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPublishersProvider : BaseProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_PUBLISHERS"

    override val defaultSortOrder = Publishers.DEFAULT_SORT

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_PUBLISHERS).whereEquals(GAME_ID, gameId)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_PUBLISHERS_JOIN_PUBLISHERS)
                .mapToTable(Publishers._ID, Tables.PUBLISHERS)
                .mapToTable(Publishers.PUBLISHER_ID, Tables.PUBLISHERS)
                .mapToTable(SyncColumns.UPDATED, Tables.PUBLISHERS)
                .whereEquals("${Tables.GAMES_PUBLISHERS}.$GAME_ID", gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_PUBLISHERS, null, values)
        return Games.buildPublisherUri(rowId)
    }
}
