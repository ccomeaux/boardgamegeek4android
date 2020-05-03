package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlaysIdPlayersProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayPlayers.CONTENT_TYPE

    override val path = "$PATH_PLAYS/#/$PATH_PLAYERS"

    override val defaultSortOrder = PlayPlayers.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        return SelectionBuilder()
                .table(Tables.PLAY_PLAYERS)
                .whereEquals(PlayPlayers._PLAY_ID, internalId)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        return SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS)
                .whereEquals(PlayPlayers._PLAY_ID, internalId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        val internalPlayId = Plays.getInternalId(uri)
        values.put(PlayPlayers._PLAY_ID, internalPlayId)
        val internalPlayerId = db.insertOrThrow(Tables.PLAY_PLAYERS, null, values)
        return Plays.buildPlayerUri(internalPlayId, internalPlayerId)
    }
}
