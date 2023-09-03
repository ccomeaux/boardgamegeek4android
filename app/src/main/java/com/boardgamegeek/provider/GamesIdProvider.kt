package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.GamePolls
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Games.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#"

    private val provider = GamesProvider()
    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
            .table(Tables.GAMES)
            .whereEquals("${Tables.GAMES}.${Games.Columns.GAME_ID}", gameId)
    }

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return provider.buildSimpleSelection(uri).whereEquals(Games.Columns.GAME_ID, gameId)
    }
}
