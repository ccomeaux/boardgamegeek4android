package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggContract.Publishers
import com.boardgamegeek.provider.BggDatabase.Tables

class PublishersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_ITEM_TYPE

    override val path = "$PATH_PUBLISHERS/#"

    private val table = Tables.PUBLISHERS

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val publisherId = Publishers.getPublisherId(uri)
        return SelectionBuilder()
            .table(table)
            .whereEquals(Publishers.Columns.PUBLISHER_ID, publisherId)
    }

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val designerId = Publishers.getPublisherId(uri)
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Publishers.Columns.PUBLISHER_ID, table)
            .mapToTable(Publishers.Columns.UPDATED, table)
            .whereEquals("$table.${Publishers.Columns.PUBLISHER_ID}", designerId)
        if (projection.orEmpty().contains(Publishers.Columns.ITEM_COUNT)) {
            builder
                .table(Tables.PUBLISHERS_JOIN_COLLECTION)
                .groupBy("$table.${Publishers.Columns.PUBLISHER_ID}")
                .mapAsCount(Publishers.Columns.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }}
