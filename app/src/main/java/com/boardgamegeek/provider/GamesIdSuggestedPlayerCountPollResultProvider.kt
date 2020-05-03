package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollResultsColumns.PLAYER_COUNT
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdSuggestedPlayerCountPollResultProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameSuggestedPlayerCountPollPollResults.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val playerCount = Games.getPollName(uri)
        return SelectionBuilder()
                .table(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
                .whereEquals(GAME_ID, gameId)
                .whereEquals(PLAYER_COUNT, playerCount)
    }
}
