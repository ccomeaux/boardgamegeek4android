package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Companion.QUERY_KEY_GROUP_BY
import com.boardgamegeek.provider.BggContract.Companion.QUERY_KEY_HAVING
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDATION
import com.boardgamegeek.provider.BggDatabase.Tables

class CollectionProvider : BasicProvider() {
    override fun getType(uri: Uri) = Collection.CONTENT_TYPE

    override val path = PATH_COLLECTION

    override val table = Tables.COLLECTION

    override val defaultSortOrder = Collection.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
            .table(Tables.COLLECTION_JOIN_GAMES)
            .mapToTable(_ID, Tables.COLLECTION)
            .mapToTable(Collection.Columns.GAME_ID, Tables.COLLECTION)
            .mapToTable(Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .mapToTable(Collection.Columns.PRIVATE_INFO_QUANTITY, Tables.COLLECTION)
            .mapIfNull(Games.Columns.GAME_RANK, CollectionItemEntity.RANK_UNKNOWN.toString()) // TODO move upstream or is this even necessary?
            .map(Plays.Columns.MAX_DATE, "(SELECT MAX(${Plays.Columns.DATE}) FROM ${Tables.PLAYS} WHERE ${Tables.PLAYS}.${Plays.Columns.OBJECT_ID}=${Tables.GAMES}.${Games.Columns.GAME_ID})")
        var groupBy = uri.getQueryParameter(QUERY_KEY_GROUP_BY).orEmpty()
        val having = uri.getQueryParameter(QUERY_KEY_HAVING).orEmpty()
        for (column in projection.orEmpty()) {
            if (column.startsWith(Games.Columns.PLAYER_COUNT_RECOMMENDATION_PREFIX)) {
                val playerCount = Games.getRecommendedPlayerCountFromColumn(column)
                if (!playerCount.isNullOrBlank()) {
                    builder.map(
                        Games.createRecommendedPlayerCountColumn(playerCount),
                        "(SELECT $RECOMMENDATION FROM ${Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS} AS x WHERE ${Tables.COLLECTION}.${Games.Columns.GAME_ID}=x.${Games.Columns.GAME_ID} AND x.player_count=$playerCount)"
                    )
                }
                if (groupBy.isEmpty()) groupBy = Games.Columns.GAME_ID
            }
        }
        if (having.isNotEmpty() && groupBy.isEmpty()) groupBy = Games.Columns.GAME_ID
        if (groupBy.isNotEmpty()) builder.groupBy(groupBy)
        builder.having(having)
        return builder
    }
}
