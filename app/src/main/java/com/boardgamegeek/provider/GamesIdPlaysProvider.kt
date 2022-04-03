package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYS
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdPlaysProvider : BaseProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_PLAYS"

    override val defaultSortOrder = Plays.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
            .table(Tables.PLAYS)
            .whereEquals(Plays.Columns.OBJECT_ID, gameId)
    }
}
