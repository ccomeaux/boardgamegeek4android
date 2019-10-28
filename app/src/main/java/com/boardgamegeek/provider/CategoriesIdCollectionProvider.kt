package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CategoriesIdCollectionProvider : BaseProvider() {
    override fun getPath() = "categories/#/collection"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        return SelectionBuilder()
                .mapToTable(BggContract.Collection._ID, Tables.COLLECTION)
                .mapToTable(BggContract.Collection.GAME_ID, Tables.GAMES)
                .table(Tables.CATEGORY_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Categories.CATEGORY_ID, categoryId)
    }
}
