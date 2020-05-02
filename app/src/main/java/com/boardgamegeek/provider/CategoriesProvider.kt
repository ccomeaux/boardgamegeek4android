package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.PATH_CATEGORIES
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CategoriesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_TYPE

    override fun getPath() = PATH_CATEGORIES

    override val table = Tables.CATEGORIES

    override fun getDefaultSortOrder(): String? = Categories.DEFAULT_SORT

    override val insertedIdColumn = Categories.CATEGORY_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Categories.CATEGORY_ID, table)
        if (projection.contains(Categories.ITEM_COUNT)) {
            builder
                    .table(Tables.CATEGORIES_JOIN_COLLECTION)
                    .groupBy("$table.${Categories.CATEGORY_ID}")
                    .mapAsCount(Categories.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
