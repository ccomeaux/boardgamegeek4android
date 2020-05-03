package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesArtists
import com.boardgamegeek.util.SelectionBuilder

class GamesIdArtistsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_ITEM_TYPE

    override val path = "${PATH_GAMES}/#/${PATH_ARTISTS}/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val artistId = ContentUris.parseId(uri)
        return SelectionBuilder()
                .table(BggDatabase.Tables.GAMES_ARTISTS)
                .whereEquals(GamesArtists.GAME_ID, gameId)
                .whereEquals(GamesArtists.ARTIST_ID, artistId)
    }
}
