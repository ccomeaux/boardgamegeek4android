package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggDatabase.Tables

class MechanicsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_ITEM_TYPE

    override val path = "$PATH_MECHANICS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val mechanicId = Mechanics.getMechanicId(uri)
        return SelectionBuilder()
            .table(Tables.MECHANICS)
            .whereEquals(Mechanics.Columns.MECHANIC_ID, mechanicId)
    }
}
