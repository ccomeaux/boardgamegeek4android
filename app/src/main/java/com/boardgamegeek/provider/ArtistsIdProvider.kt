package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggDatabase.Tables

class ArtistsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_ITEM_TYPE

    override val path = "$PATH_ARTISTS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        return SelectionBuilder()
            .table(Tables.ARTISTS)
            .whereEquals(Artists.Columns.ARTIST_ID, artistId)
    }
}
