package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class ArtistsProvider : BasicProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_TYPE

    override fun getPath() = BggContract.PATH_ARTISTS

    override fun getTable() = Tables.ARTISTS

    override fun getDefaultSortOrder(): String? = Artists.DEFAULT_SORT

    override fun getInsertedIdColumn(): String? = Artists.ARTIST_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Artists.ARTIST_ID, table)
                .mapToTable(Artists.UPDATED, table)
        if (projection.contains(Artists.ITEM_COUNT)) {
            builder
                    .table(Tables.ARTISTS_JOIN_COLLECTION)
                    .groupBy("$table.${Artists.ARTIST_ID}")
                    .mapAsCount(Artists.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
