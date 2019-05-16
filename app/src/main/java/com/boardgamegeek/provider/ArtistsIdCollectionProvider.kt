package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class ArtistsIdCollectionProvider : BaseProvider() {
    override fun getPath() = "artists/#/collection"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        return SelectionBuilder()
                .table(Tables.ARTIST_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Artists.ARTIST_ID, artistId)
    }
}
