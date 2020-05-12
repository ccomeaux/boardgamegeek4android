package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.entities.RANK_UNKNOWN
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Games.CONTENT_TYPE

    override val path = PATH_GAMES

    override val table = Tables.GAMES

    override val defaultSortOrder = Games.DEFAULT_SORT

    override val insertedIdColumn = Games.GAME_ID

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val builder = SelectionBuilder()
        if (FRAGMENT_PLAYS == uri.fragment) {
            builder
                    .table(Tables.GAMES_JOIN_PLAYS)
                    .mapIfNull(Games.GAME_RANK, RANK_UNKNOWN.toString())
                    .mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY, Tables.PLAYS)
                    .mapAsMax(Plays.MAX_DATE, Plays.DATE)
        } else {
            builder
                    .table(Tables.GAMES_JOIN_COLLECTION)
                    .mapToTable(Games.GAME_ID, Tables.GAMES)
        }
        builder.mapToTable(Games.UPDATED, Tables.GAMES)
        val groupBy = uri.getQueryParameter(QUERY_KEY_GROUP_BY).orEmpty()
        if (groupBy.isNotEmpty()) {
            builder.groupBy(groupBy)
        } else {
            builder.groupBy(Games.GAME_ID)
        }
        return builder
    }
}