package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlaysProvider : BasicProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_TYPE

    override val path = PATH_PLAYS

    override val table = Tables.PLAYS

    override val defaultSortOrder = Plays.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val builder = SelectionBuilder()
                .table(if (FRAGMENT_SIMPLE == uri.fragment) Tables.PLAYS else Tables.PLAYS_JOIN_GAMES)
                .mapToTable(Plays._ID, Tables.PLAYS)
                .mapToTable(Plays.PLAY_ID, Tables.PLAYS)
                .mapToTable(Plays.SYNC_TIMESTAMP, Tables.PLAYS)
                .mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY)
                .mapAsMax(Plays.MAX_DATE, Plays.DATE)
        val groupBy = uri.getQueryParameter(QUERY_KEY_GROUP_BY).orEmpty()
        if (groupBy.isNotEmpty()) builder.groupBy(groupBy)
        return builder
    }
}