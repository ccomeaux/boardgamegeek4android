package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlaysIdPlayersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayPlayers.CONTENT_ITEM_TYPE

    override val path = "$PATH_PLAYS/#/$PATH_PLAYERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        val rowId = PlayPlayers.getPlayPlayerId(uri)
        return SelectionBuilder()
                .table(Tables.PLAY_PLAYERS)
                .whereEquals(PlayPlayers._PLAY_ID, internalId)
                .whereEquals(PlayPlayers._ID, rowId)
    }
}
