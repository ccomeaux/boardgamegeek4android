package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesCategoriesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_CATEGORIES/#"

    override val defaultSortOrder = Categories.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val id = ContentUris.parseId(uri)
        return SelectionBuilder().table(Tables.GAMES_CATEGORIES).whereEquals(BaseColumns._ID, id)
    }
}
