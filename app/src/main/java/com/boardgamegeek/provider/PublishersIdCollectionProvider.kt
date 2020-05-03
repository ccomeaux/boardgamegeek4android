package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PublishersIdCollectionProvider : BaseProvider() {
    override val path = "$PATH_PUBLISHERS/#/$PATH_COLLECTION"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val publisherId = Publishers.getPublisherId(uri)
        return SelectionBuilder()
                .mapToTable(_ID, Tables.COLLECTION)
                .mapToTable(GAME_ID, Tables.GAMES)
                .table(Tables.PUBLISHER_JOIN_GAMES_JOIN_COLLECTION)
                .whereEquals(Publishers.PUBLISHER_ID, publisherId)
    }
}
