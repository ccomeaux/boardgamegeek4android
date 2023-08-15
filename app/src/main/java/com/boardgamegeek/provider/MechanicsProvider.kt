package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns

import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggDatabase.Tables

class MechanicsProvider : BasicProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_TYPE

    override val path = PATH_MECHANICS

    override val table = Tables.MECHANICS

    override val defaultSortOrder = Mechanics.DEFAULT_SORT

    override val insertedIdColumn = Mechanics.Columns.MECHANIC_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Mechanics.Columns.MECHANIC_ID, table)
        if (projection.orEmpty().contains(Mechanics.Columns.ITEM_COUNT)) {
            builder
                .table(Tables.MECHANICS_JOIN_COLLECTION)
                .groupBy("$table.${Mechanics.Columns.MECHANIC_ID}")
                .mapAsCount(Mechanics.Columns.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
