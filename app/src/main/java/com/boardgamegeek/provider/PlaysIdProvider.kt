package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.FRAGMENT_SIMPLE
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class PlaysIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_ITEM_TYPE

    override val path = "$PATH_PLAYS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        return SelectionBuilder()
            .table(if (FRAGMENT_SIMPLE == uri.fragment) Tables.PLAYS else Tables.PLAYS_JOIN_GAMES)
            .mapToTable(BaseColumns._ID, Tables.PLAYS)
            .mapToTable(Plays.Columns.PLAY_ID, Tables.PLAYS)
            .mapToTable(Plays.Columns.SYNC_TIMESTAMP, Tables.PLAYS)
            .whereEquals("${Tables.PLAYS}.${BaseColumns._ID}", internalId.toString())
    }
}
