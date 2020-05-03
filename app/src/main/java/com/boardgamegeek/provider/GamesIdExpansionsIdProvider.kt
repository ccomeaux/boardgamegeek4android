package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggContract.GamesExpansionsColumns.EXPANSION_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdExpansionsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamesExpansions.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_EXPANSIONS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val expansionId = ContentUris.parseId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_EXPANSIONS)
                .whereEquals(GAME_ID, gameId)
                .whereEquals(EXPANSION_ID, expansionId)
    }
}
