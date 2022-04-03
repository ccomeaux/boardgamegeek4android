package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.FRAGMENT_SIMPLE
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.Companion.QUERY_KEY_GROUP_BY
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class PlaysProvider : BasicProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_TYPE

    override val path = PATH_PLAYS

    override val table = Tables.PLAYS

    override val defaultSortOrder = Plays.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val builder = SelectionBuilder()
            .table(if (FRAGMENT_SIMPLE == uri.fragment) Tables.PLAYS else Tables.PLAYS_JOIN_GAMES)
            .mapToTable(BaseColumns._ID, Tables.PLAYS)
            .mapToTable(Plays.Columns.PLAY_ID, Tables.PLAYS)
            .mapToTable(Plays.Columns.SYNC_TIMESTAMP, Tables.PLAYS)
            .mapAsSum(Plays.Columns.SUM_QUANTITY, Plays.Columns.QUANTITY)
            .mapAsMax(Plays.Columns.MAX_DATE, Plays.Columns.DATE)
        val groupBy = uri.getQueryParameter(QUERY_KEY_GROUP_BY).orEmpty()
        if (groupBy.isNotEmpty()) builder.groupBy(groupBy)
        return builder
    }
}
