package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYERS
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class PlaysIdPlayersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayPlayers.CONTENT_ITEM_TYPE

    override val path = "$PATH_PLAYS/#/$PATH_PLAYERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        val rowId = PlayPlayers.getPlayPlayerId(uri)
        return SelectionBuilder()
            .table(Tables.PLAY_PLAYERS)
            .whereEquals(PlayPlayers.Columns._PLAY_ID, internalId)
            .whereEquals(BaseColumns._ID, rowId)
    }
}
