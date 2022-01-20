package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdSuggestedPlayerCountPollResultProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameSuggestedPlayerCountPollPollResults.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val playerCount = Games.getPollPlayerCount(uri)
        return SelectionBuilder()
            .table(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
            .whereEquals(GameSuggestedPlayerCountPollPollResults.Columns.GAME_ID, gameId)
            .whereEquals(GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT, playerCount)
    }
}
