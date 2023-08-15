package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggDatabase.Tables

class MechanicsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_ITEM_TYPE

    override val path = "$PATH_MECHANICS/#"

    private val table = Tables.MECHANICS

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val mechanicId = Mechanics.getMechanicId(uri)
        return SelectionBuilder()
            .table(table)
            .whereEquals(Mechanics.Columns.MECHANIC_ID, mechanicId)
    }

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        val mechanicId = Mechanics.getMechanicId(uri)
        val builder = SelectionBuilder()
            .mapToTable(BaseColumns._ID, table)
            .mapToTable(Mechanics.Columns.MECHANIC_ID, table)
            .whereEquals(Mechanics.Columns.MECHANIC_ID, mechanicId)
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
