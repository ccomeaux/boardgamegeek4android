package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesArtistsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_ARTISTS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val id = ContentUris.parseId(uri)
        return SelectionBuilder().table(Tables.GAMES_ARTISTS).whereEquals(BaseColumns._ID, id)
    }
}
