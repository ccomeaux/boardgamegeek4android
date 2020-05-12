package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.entities.RANK_UNKNOWN
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.CollectionColumns.PRIVATE_INFO_QUANTITY
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollResultsColumns.RECOMMENDATION
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggContract.SyncColumns.UPDATED
import com.boardgamegeek.provider.BggContract.SyncListColumns.UPDATED_LIST
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionProvider : BasicProvider() {
    override fun getType(uri: Uri) = BggContract.Collection.CONTENT_TYPE

    override val path = PATH_COLLECTION

    override val table = Tables.COLLECTION

    override val defaultSortOrder = BggContract.Collection.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
                .table(Tables.COLLECTION_JOIN_GAMES)
                .mapToTable(_ID, Tables.COLLECTION)
                .mapToTable(GAME_ID, Tables.COLLECTION)
                .mapToTable(UPDATED, Tables.COLLECTION)
                .mapToTable(UPDATED_LIST, Tables.COLLECTION)
                .mapToTable(PRIVATE_INFO_QUANTITY, Tables.COLLECTION)
                .mapIfNull(Games.GAME_RANK, RANK_UNKNOWN.toString())
                .map(Plays.MAX_DATE, "(SELECT MAX(${Plays.DATE}) FROM ${Tables.PLAYS} WHERE ${Tables.PLAYS}.${Plays.OBJECT_ID}=${Tables.GAMES}.$GAME_ID)")
        var groupBy = uri.getQueryParameter(QUERY_KEY_GROUP_BY).orEmpty()
        val having = uri.getQueryParameter(QUERY_KEY_HAVING).orEmpty()
        for (column in projection.orEmpty()) {
            if (column.startsWith(Games.PLAYER_COUNT_RECOMMENDATION_PREFIX)) {
                val playerCount = Games.getRecommendedPlayerCountFromColumn(column)
                if (playerCount.isNotEmpty()) {
                    builder.map(Games.createRecommendedPlayerCountColumn(playerCount),
                            "(SELECT $RECOMMENDATION FROM ${Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS} AS x WHERE ${Tables.COLLECTION}.$GAME_ID=x.$GAME_ID AND x.player_count=$playerCount)")
                }
                if (groupBy.isEmpty()) groupBy = GAME_ID
            }
        }
        if (having.isNotEmpty() && groupBy.isEmpty()) groupBy = GAME_ID
        if (groupBy.isNotEmpty()) builder.groupBy(groupBy)
        builder.having(having)
        return builder
    }
}