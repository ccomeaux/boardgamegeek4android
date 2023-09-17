package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.Companion.PATH_CATEGORIES
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggDatabase.Tables

class CategoriesIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_CATEGORIES/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        return SelectionBuilder()
            .mapToTable(BaseColumns._ID, Tables.COLLECTION)
            .mapToTable(Games.Columns.GAME_ID, Tables.GAMES)
            .mapToTable(BggContract.Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(BggContract.Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .table(Tables.CATEGORY_JOIN_GAMES_JOIN_COLLECTION)
            .whereEquals(Categories.Columns.CATEGORY_ID, categoryId)
    }
}
