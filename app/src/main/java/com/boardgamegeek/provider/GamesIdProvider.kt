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
        val gamePollsCount = "(SELECT COUNT(${GamePolls.Columns.POLL_NAME}) FROM ${Tables.GAME_POLLS} WHERE ${Tables.GAME_POLLS}.${GamePolls.Columns.GAME_ID}=${Tables.GAMES}.${Games.Columns.GAME_ID})"
        return SelectionBuilder()
            .table(Tables.GAMES)
            .map(Games.Columns.POLLS_COUNT, gamePollsCount)
            .whereEquals("${Tables.GAMES}.${Games.Columns.GAME_ID}", gameId)
    }

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return provider.buildSimpleSelection(uri).whereEquals(Games.Columns.GAME_ID, gameId)
    }
}
