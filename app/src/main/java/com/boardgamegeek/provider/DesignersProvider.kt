package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_DESIGNERS
import com.boardgamegeek.provider.BggContract.Designers
import com.boardgamegeek.provider.BggDatabase.Tables

class DesignersProvider : BasicProvider() {
    override fun getType(uri: Uri) = Designers.CONTENT_TYPE

    override val path = PATH_DESIGNERS

    override val table = Tables.DESIGNERS

    override val defaultSortOrder = Designers.DEFAULT_SORT

    override val insertedIdColumn = Designers.Columns.DESIGNER_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
            .mapToTable(Designers.Columns.DESIGNER_ID, table)
            .mapToTable(Designers.Columns.UPDATED, table)
        if (projection.orEmpty().contains(Designers.Columns.ITEM_COUNT)) {
            builder
                .table(Tables.DESIGNERS_JOIN_COLLECTION)
                .groupBy("$table.${Designers.Columns.DESIGNER_ID}")
                .mapAsCount(Designers.Columns.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
