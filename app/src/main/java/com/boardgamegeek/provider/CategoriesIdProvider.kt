package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Categories
import com.boardgamegeek.provider.BggContract.Companion.PATH_CATEGORIES
import com.boardgamegeek.provider.BggDatabase.Tables

class CategoriesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_ITEM_TYPE

    override val path = "$PATH_CATEGORIES/#"

    private val table = Tables.CATEGORIES

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        return SelectionBuilder()
            .table(table)
            .whereEquals(Categories.Columns.CATEGORY_ID, categoryId)
    }

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val categoryId = Categories.getCategoryId(uri)
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Categories.Columns.CATEGORY_ID, table)
            .whereEquals(Categories.Columns.CATEGORY_ID, categoryId)
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
