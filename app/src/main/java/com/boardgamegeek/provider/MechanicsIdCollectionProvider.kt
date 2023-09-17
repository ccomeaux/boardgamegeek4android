package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggDatabase.Tables

class MechanicsIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_MECHANICS/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val mechanicId = Mechanics.getMechanicId(uri)
        return SelectionBuilder()
            .mapToTable(BaseColumns._ID, Tables.COLLECTION)
            .mapToTable(Games.Columns.GAME_ID, Tables.GAMES)
            .mapToTable(BggContract.Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(BggContract.Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .table(Tables.MECHANIC_JOIN_GAMES_JOIN_COLLECTION)
            .whereEquals(Mechanics.Columns.MECHANIC_ID, mechanicId)
    }
}
