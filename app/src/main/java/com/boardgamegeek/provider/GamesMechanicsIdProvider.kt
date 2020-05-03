package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesMechanicsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_MECHANICS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val id = ContentUris.parseId(uri)
        return SelectionBuilder().table(Tables.GAMES_MECHANICS).whereEquals(BaseColumns._ID, id)
    }
}
