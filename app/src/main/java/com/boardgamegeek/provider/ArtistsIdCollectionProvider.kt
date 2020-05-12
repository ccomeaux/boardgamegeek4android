package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*

import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class ArtistsIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_ARTISTS/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        return SelectionBuilder()
                .mapToTable(_ID, Tables.COLLECTION)
                .mapToTable(GamesColumns.GAME_ID, Tables.GAMES)
                .table(Tables.ARTIST_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Artists.ARTIST_ID, artistId)
    }
}
