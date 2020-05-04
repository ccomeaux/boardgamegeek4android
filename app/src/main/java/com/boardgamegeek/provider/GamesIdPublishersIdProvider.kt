package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesPublishers.GAME_ID
import com.boardgamegeek.provider.BggDatabase.GamesPublishers.PUBLISHER_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPublishersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_PUBLISHERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val publisherId = ContentUris.parseId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_PUBLISHERS)
                .whereEquals(GAME_ID, gameId)
                .whereEquals(PUBLISHER_ID, publisherId)
    }
}
