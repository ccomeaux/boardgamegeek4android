package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class MechanicsProvider : BasicProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_TYPE

    override fun getPath() = BggContract.PATH_MECHANICS

    override fun getTable() = Tables.MECHANICS

    override fun getDefaultSortOrder(): String? = Mechanics.DEFAULT_SORT

    override fun getInsertedIdColumn(): String? = Mechanics.MECHANIC_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Mechanics.MECHANIC_ID, table)
        if (projection.contains(Mechanics.ITEM_COUNT)) {
            builder
                    .table(Tables.MECHANICS_JOIN_COLLECTION)
                    .groupBy("$table.${Mechanics.MECHANIC_ID}")
                    .mapAsCount(Mechanics.ITEM_COUNT)
        } else {
            builder.table(table)
        }
        return builder
    }
}
