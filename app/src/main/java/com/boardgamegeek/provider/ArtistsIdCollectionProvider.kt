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
                .mapToTable(BggContract.Collection._ID, Tables.COLLECTION)
                .mapToTable(BggContract.Collection.GAME_ID, Tables.GAMES)
                .table(Tables.ARTIST_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Artists.ARTIST_ID, artistId)
    }
}
