package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggContract.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.SyncColumns.UPDATED
import com.boardgamegeek.provider.BggContract.SyncListColumns.UPDATED_LIST
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = BggContract.Collection.CONTENT_ITEM_TYPE

    override val path = "$PATH_COLLECTION/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val id = BggContract.Collection.getId(uri)
        return SelectionBuilder()
                .table(Tables.COLLECTION)
                .whereEquals(_ID, id)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val id = BggContract.Collection.getId(uri)
        return SelectionBuilder().table(Tables.COLLECTION_JOIN_GAMES).mapToTable(_ID, Tables.COLLECTION)
                .mapToTable(GAME_ID, Tables.COLLECTION).mapToTable(UPDATED, Tables.COLLECTION)
                .mapToTable(UPDATED_LIST, Tables.COLLECTION)
                .whereEquals("${Tables.COLLECTION}.$_ID", id)
    }

}