package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggContract.PATH_MECHANICS
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class MechanicsProvider : BasicProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_TYPE

    override val path = PATH_MECHANICS

    override val table = Tables.MECHANICS

    override val defaultSortOrder = Mechanics.DEFAULT_SORT

    override val insertedIdColumn = Mechanics.MECHANIC_ID

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val builder = SelectionBuilder()
                .mapToTable(Mechanics.MECHANIC_ID, table)
        if (projection.orEmpty().contains(Mechanics.ITEM_COUNT)) {
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
