package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Designers
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class DesignersProvider : BasicProvider() {
    override fun getType(uri: Uri) = Designers.CONTENT_TYPE

    override fun getPath() = BggContract.PATH_DESIGNERS

    override fun getTable() = Tables.DESIGNERS

    override fun getDefaultSortOrder(): String? = Designers.DEFAULT_SORT

    override fun getInsertedIdColumn(): String? = Designers.DESIGNER_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Designers.DESIGNER_ID, table)
                .mapToTable(Designers.UPDATED, table)
        if (projection.contains(Designers.ITEM_COUNT)) {
            builder
                    .table(Tables.DESIGNERS_JOIN_COLLECTION)
                    .groupBy("$table.${Designers.DESIGNER_ID}")
                    .mapAsCount(Designers.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
