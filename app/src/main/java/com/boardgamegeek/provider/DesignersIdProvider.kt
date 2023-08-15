package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_DESIGNERS
import com.boardgamegeek.provider.BggContract.Designers
import com.boardgamegeek.provider.BggDatabase.Tables

class DesignersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Designers.CONTENT_ITEM_TYPE

    override val path = "$PATH_DESIGNERS/#"

    private val table = Tables.DESIGNERS

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val designerId = Designers.getDesignerId(uri)
        return SelectionBuilder().table(table).whereEquals(Designers.Columns.DESIGNER_ID, designerId)
    }

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val designerId = Designers.getDesignerId(uri)
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Designers.Columns.DESIGNER_ID, table)
            .mapToTable(Designers.Columns.UPDATED, table)
            .whereEquals("$table.${Designers.Columns.DESIGNER_ID}", designerId)
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
