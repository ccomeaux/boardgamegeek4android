package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_EXPANSIONS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.GamesExpansions
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdExpansionsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamesExpansions.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_EXPANSIONS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val expansionId = ContentUris.parseId(uri)
        return SelectionBuilder()
            .table(Tables.GAMES_EXPANSIONS)
            .whereEquals(GamesExpansions.Columns.GAME_ID, gameId)
            .whereEquals(GamesExpansions.Columns.EXPANSION_ID, expansionId)
    }
}
