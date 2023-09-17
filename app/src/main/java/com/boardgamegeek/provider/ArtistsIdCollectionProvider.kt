package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Artists
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggDatabase.Tables

class ArtistsIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_ARTISTS/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val artistId = Artists.getArtistId(uri)
        return SelectionBuilder()
            .mapToTable(BaseColumns._ID, Tables.COLLECTION)
            .mapToTable(Artists.Columns.GAME_ID, Tables.GAMES)
            .mapToTable(BggContract.Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(BggContract.Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .table(Tables.ARTIST_JOIN_GAMES_JOIN_COLLECTION)
            .whereEquals(Artists.Columns.ARTIST_ID, artistId)
    }
}
