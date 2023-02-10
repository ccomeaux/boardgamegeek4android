package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggDatabase.Tables

class CollectionProvider : BasicProvider() {
    override fun getType(uri: Uri) = Collection.CONTENT_TYPE

    override val path = PATH_COLLECTION

    override val table = Tables.COLLECTION

    override val defaultSortOrder = Collection.DEFAULT_SORT

    override fun buildExpandedSelection(uri: Uri, projection: Array<String>?): SelectionBuilder {
        return SelectionBuilder()
            .table(Tables.COLLECTION_JOIN_GAMES)
            .mapToTable(BaseColumns._ID, Tables.COLLECTION)
            .mapToTable(Collection.Columns.GAME_ID, Tables.COLLECTION)
            .mapToTable(Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .mapToTable(Collection.Columns.PRIVATE_INFO_QUANTITY, Tables.COLLECTION)
            .map(
                Plays.Columns.MAX_DATE,
                "(SELECT MAX(${Plays.Columns.DATE}) FROM ${Tables.PLAYS} WHERE ${Tables.PLAYS}.${Plays.Columns.OBJECT_ID}=${Tables.GAMES}.${Games.Columns.GAME_ID})"
            )
    }
}
