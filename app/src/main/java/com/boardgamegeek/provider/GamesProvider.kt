package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.FRAGMENT_PLAYS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Games.CONTENT_TYPE

    override val path = PATH_GAMES

    override val table = Tables.GAMES

    override val defaultSortOrder = Games.DEFAULT_SORT

    override val insertedIdColumn = Games.Columns.GAME_ID

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val builder = SelectionBuilder()
        if (FRAGMENT_PLAYS == uri.fragment) {
            builder
                .table(Tables.GAMES_JOIN_PLAYS)
                .mapAsSum(Plays.Columns.SUM_QUANTITY, Plays.Columns.QUANTITY, Tables.PLAYS)
                .mapAsMax(Plays.Columns.MAX_DATE, Plays.Columns.DATE)
        } else {
            builder
                .table(Tables.GAMES_JOIN_COLLECTION)
                .mapToTable(Games.Columns.GAME_ID, Tables.GAMES)
        }
        return builder
            .mapToTable(Games.Columns.UPDATED, Tables.GAMES)
            .groupBy(Games.Columns.GAME_ID)
    }
}
