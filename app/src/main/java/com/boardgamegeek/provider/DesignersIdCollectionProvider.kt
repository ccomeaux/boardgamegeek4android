package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Companion.PATH_DESIGNERS

import com.boardgamegeek.provider.BggDatabase.Tables

class DesignersIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_DESIGNERS/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val designerId = Designers.getDesignerId(uri)
        return SelectionBuilder()
            .mapToTable(BaseColumns._ID, Tables.COLLECTION)
            .mapToTable(Games.Columns.GAME_ID, Tables.GAMES)
            .table(Tables.DESIGNER_JOIN_GAMES_JOIN_COLLECTION)
            .whereEquals(Designers.Columns.DESIGNER_ID, designerId)
    }
}
