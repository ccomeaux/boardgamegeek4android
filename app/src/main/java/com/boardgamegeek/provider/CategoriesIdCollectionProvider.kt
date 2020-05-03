package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CategoriesIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_CATEGORIES/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        return SelectionBuilder()
                .mapToTable(_ID, Tables.COLLECTION)
                .mapToTable(GAME_ID, Tables.GAMES)
                .table(Tables.CATEGORY_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Categories.CATEGORY_ID, categoryId)
    }
}
