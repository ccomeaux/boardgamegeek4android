package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Publishers
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PublishersIdCollectionProvider : BaseProvider() {
    override fun getPath() = "publishers/#/collection"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val publisherId = Publishers.getPublisherId(uri)
        return SelectionBuilder()
                .mapToTable(BggContract.Collection._ID, Tables.COLLECTION)
                .mapToTable(BggContract.Collection.GAME_ID, Tables.GAMES)
                .table(Tables.PUBLISHER_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Publishers.PUBLISHER_ID, publisherId)
    }
}
