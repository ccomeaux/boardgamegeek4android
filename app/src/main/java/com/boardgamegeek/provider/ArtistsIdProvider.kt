package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggContract.PATH_ARTISTS
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class ArtistsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_ITEM_TYPE

    override val path = "$PATH_ARTISTS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        return SelectionBuilder()
                .table(Tables.ARTISTS)
                .whereEquals(Artists.ARTIST_ID, artistId)
    }
}
