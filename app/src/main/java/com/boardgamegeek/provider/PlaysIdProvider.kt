package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class PlaysIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_ITEM_TYPE

    override val path = "$PATH_PLAYS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        return SelectionBuilder()
            .table(if (BggContract.FRAGMENT_SIMPLE == uri.fragment) Tables.PLAYS else Tables.PLAYS_JOIN_GAMES)
            .mapToTable(Plays._ID, Tables.PLAYS)
            .mapToTable(Plays.PLAY_ID, Tables.PLAYS)
            .mapToTable(Plays.SYNC_TIMESTAMP, Tables.PLAYS)
            .whereEquals("${Tables.PLAYS}.${Plays._ID}", internalId.toString())
    }
}
