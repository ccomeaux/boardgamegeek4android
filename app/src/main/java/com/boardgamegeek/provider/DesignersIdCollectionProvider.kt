package com.boardgamegeek.provider

import android.net.Uri

import com.boardgamegeek.provider.BggContract.Designers
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class DesignersIdCollectionProvider : BaseProvider() {
    override fun getPath() = "designers/#/collection"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val designerId = Designers.getDesignerId(uri)
        return SelectionBuilder()
                .mapToTable(BggContract.Collection._ID, Tables.COLLECTION)
                .mapToTable(BggContract.Collection.GAME_ID, Tables.GAMES)
                .table(Tables.DESIGNER_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Designers.DESIGNER_ID, designerId)
    }
}
