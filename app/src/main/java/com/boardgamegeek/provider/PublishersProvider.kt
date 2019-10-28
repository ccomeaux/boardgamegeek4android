package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Publishers
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PublishersProvider : BasicProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_TYPE

    override fun getPath() = BggContract.PATH_PUBLISHERS

    override fun getTable() = Tables.PUBLISHERS

    override fun getDefaultSortOrder(): String? = Publishers.DEFAULT_SORT

    override fun getInsertedIdColumn(): String? = Publishers.PUBLISHER_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Publishers.PUBLISHER_ID, table)
                .mapToTable(Publishers.UPDATED, table)
        if (projection.contains(Publishers.ITEM_COUNT)) {
            builder
                    .table(Tables.PUBLISHERS_JOIN_COLLECTION)
                    .groupBy("$table.${Publishers.PUBLISHER_ID}")
                    .mapAsCount(Publishers.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
