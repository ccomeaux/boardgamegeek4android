package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggDatabase.Tables

class CollectionIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Collection.CONTENT_ITEM_TYPE

    override val path = "$PATH_COLLECTION/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val id = Collection.getId(uri)
        return SelectionBuilder()
            .table(Tables.COLLECTION)
            .whereEquals(BaseColumns._ID, id)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val id = Collection.getId(uri)
        return SelectionBuilder()
            .table(Tables.COLLECTION_JOIN_GAMES)
            .mapToTable(BaseColumns._ID, Tables.COLLECTION)
            .mapToTable(Collection.Columns.GAME_ID, Tables.COLLECTION)
            .mapToTable(Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .map(BggContract.Plays.Columns.MAX_DATE, "(SELECT MAX(${BggContract.Plays.Columns.DATE}) FROM ${Tables.PLAYS} WHERE ${Tables.PLAYS}.${BggContract.Plays.Columns.OBJECT_ID}=${Tables.GAMES}.${BggContract.Games.Columns.GAME_ID})")
            .whereEquals("${Tables.COLLECTION}.${BaseColumns._ID}", id)
    }

}
