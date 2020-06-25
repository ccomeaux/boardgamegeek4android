package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.PATH_CATEGORIES
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CategoriesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_TYPE

    override val path = PATH_CATEGORIES

    override val table = Tables.CATEGORIES

    override val defaultSortOrder = Categories.DEFAULT_SORT

    override val insertedIdColumn = Categories.CATEGORY_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Categories.CATEGORY_ID, table)
        if (projection.orEmpty().contains(Categories.ITEM_COUNT)) {
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
