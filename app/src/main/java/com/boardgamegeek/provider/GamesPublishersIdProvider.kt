package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesPublishersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_PUBLISHERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
            .table(Tables.GAMES_PUBLISHERS)
            .whereEquals(BaseColumns._ID, ContentUris.parseId(uri))
    }
}
