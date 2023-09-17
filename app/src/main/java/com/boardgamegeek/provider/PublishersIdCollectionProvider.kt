package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.Companion.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggDatabase.Tables

class PublishersIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_PUBLISHERS/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val publisherId = Publishers.getPublisherId(uri)
        return SelectionBuilder()
            .mapToTable(_ID, Tables.COLLECTION)
            .mapToTable(Games.Columns.GAME_ID, Tables.GAMES)
            .mapToTable(BggContract.Collection.Columns.UPDATED, Tables.COLLECTION)
            .mapToTable(BggContract.Collection.Columns.UPDATED_LIST, Tables.COLLECTION)
            .table(Tables.PUBLISHER_JOIN_GAMES_JOIN_COLLECTION)
            .whereEquals(Publishers.Columns.PUBLISHER_ID, publisherId)
    }
}
