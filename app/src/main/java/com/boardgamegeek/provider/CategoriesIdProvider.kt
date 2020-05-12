package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.PATH_CATEGORIES
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CategoriesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_ITEM_TYPE

    override val path = "$PATH_CATEGORIES/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        return SelectionBuilder()
                .table(Tables.CATEGORIES)
                .whereEquals(Categories.CATEGORY_ID, categoryId)
    }
}
