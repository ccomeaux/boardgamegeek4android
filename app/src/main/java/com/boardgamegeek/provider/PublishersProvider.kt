package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggContract.Publishers
import com.boardgamegeek.provider.BggDatabase.Tables

class PublishersProvider : BasicProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_TYPE

    override val path = PATH_PUBLISHERS

    override val table = Tables.PUBLISHERS

    override val defaultSortOrder = Publishers.DEFAULT_SORT

    override val insertedIdColumn = Publishers.Columns.PUBLISHER_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Publishers.Columns.PUBLISHER_ID, table)
            .mapToTable(Publishers.Columns.UPDATED, table)
        if (projection.orEmpty().contains(Publishers.Columns.ITEM_COUNT)) {
            builder
                .table(Tables.PUBLISHERS_JOIN_COLLECTION)
                .groupBy("$table.${Publishers.Columns.PUBLISHER_ID}")
                .mapAsCount(Publishers.Columns.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
