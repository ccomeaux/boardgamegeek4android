package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns

import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggDatabase.Tables

class ArtistsProvider : BasicProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_TYPE

    override val path = PATH_ARTISTS

    override val table = Tables.ARTISTS

    override val defaultSortOrder = Artists.DEFAULT_SORT

    override val insertedIdColumn = Artists.Columns.ARTIST_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Artists.Columns.ARTIST_ID, table)
            .mapToTable(Artists.Columns.UPDATED, table)
        if (projection.orEmpty().contains(Artists.Columns.ITEM_COUNT)) {
            builder
                .table(Tables.ARTISTS_JOIN_COLLECTION)
                .groupBy("$table.${Artists.Columns.ARTIST_ID}")
                .mapAsCount(Artists.Columns.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
