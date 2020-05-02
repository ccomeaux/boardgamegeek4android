package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPlaysProvider : BaseProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_TYPE

    override fun getPath() = "$PATH_GAMES/#/$PATH_PLAYS"

    override fun getDefaultSortOrder() = Plays.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.PLAYS)
                .whereEquals(Plays.OBJECT_ID, gameId)
    }
}
