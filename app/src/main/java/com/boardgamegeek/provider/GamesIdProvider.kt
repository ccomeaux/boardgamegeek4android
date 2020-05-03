package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.GamePollsColumns.POLL_NAME
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggContract.PATH_GAMES
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Games.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#"

    private val provider = GamesProvider()
    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val gamePollsCount = "(SELECT COUNT($POLL_NAME) FROM ${Tables.GAME_POLLS} WHERE ${Tables.GAME_POLLS}.$GAME_ID=${Tables.GAMES}.$GAME_ID)"
        return SelectionBuilder()
                .table(Tables.GAMES)
                .map(Games.POLLS_COUNT, gamePollsCount)
                .whereEquals("${Tables.GAMES}.$GAME_ID", gameId)
    }

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return provider.buildSimpleSelection(uri).whereEquals(Games.GAME_ID, gameId)
    }
}
