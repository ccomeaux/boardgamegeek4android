package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns

import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.Companion.PATH_CATEGORIES
import com.boardgamegeek.provider.BggDatabase.Tables

class CategoriesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_TYPE

    override val path = PATH_CATEGORIES

    override val table = Tables.CATEGORIES

    override val defaultSortOrder = Categories.DEFAULT_SORT

    override val insertedIdColumn = Categories.Columns.CATEGORY_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Categories.Columns.CATEGORY_ID, table)
        if (projection.orEmpty().contains(Categories.Columns.ITEM_COUNT)) {
            builder
                .table(Tables.CATEGORIES_JOIN_COLLECTION)
                .groupBy("$table.${Categories.Columns.CATEGORY_ID}")
                .mapAsCount(Categories.Columns.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
