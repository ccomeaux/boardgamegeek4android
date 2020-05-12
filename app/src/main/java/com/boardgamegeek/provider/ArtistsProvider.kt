package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggContract.PATH_ARTISTS
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class ArtistsProvider : BasicProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_TYPE

    override val path = PATH_ARTISTS

    override val table = Tables.ARTISTS

    override val defaultSortOrder = Artists.DEFAULT_SORT

    override val insertedIdColumn = Artists.ARTIST_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Artists.ARTIST_ID, table)
                .mapToTable(Artists.UPDATED, table)
        if (projection.orEmpty().contains(Artists.ITEM_COUNT)) {
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
