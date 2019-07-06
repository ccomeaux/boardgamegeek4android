package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Mechanics
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class MechanicsIdCollectionProvider : BaseProvider() {
    override fun getPath() = "mechanics/#/collection"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val mechanicId = Mechanics.getMechanicId(uri)
        return SelectionBuilder()
                .mapToTable(BggContract.Collection._ID, Tables.COLLECTION)
                .mapToTable(BggContract.Collection.GAME_ID, Tables.GAMES)
                .table(Tables.MECHANIC_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Mechanics.MECHANIC_ID, mechanicId)
    }
}
