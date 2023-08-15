package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggDatabase.Tables

class ArtistsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_ITEM_TYPE

    override val path = "$PATH_ARTISTS/#"

    private val table = Tables.ARTISTS

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        return SelectionBuilder()
            .table(table)
            .whereEquals(Artists.Columns.ARTIST_ID, artistId)
    }

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Artists.Columns.ARTIST_ID, table)
            .mapToTable(Artists.Columns.UPDATED, table)
            .whereEquals("$table.${Artists.Columns.ARTIST_ID}", artistId)
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
