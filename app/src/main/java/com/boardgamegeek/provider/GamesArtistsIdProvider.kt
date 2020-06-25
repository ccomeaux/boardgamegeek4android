package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.PATH_ARTISTS
import com.boardgamegeek.provider.BggContract.PATH_GAMES
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesArtistsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = BggContract.Artists.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_ARTISTS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val id = ContentUris.parseId(uri)
        return SelectionBuilder().table(Tables.GAMES_ARTISTS).whereEquals(BaseColumns._ID, id)
    }
}
