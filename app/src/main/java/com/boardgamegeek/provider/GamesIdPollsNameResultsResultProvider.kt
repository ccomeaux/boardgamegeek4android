package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_POLLS
import com.boardgamegeek.provider.BggContract.Companion.PATH_POLL_RESULTS
import com.boardgamegeek.provider.BggContract.Companion.PATH_POLL_RESULTS_RESULT
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdPollsNameResultsResultProvider : BaseProvider() {
    override val path = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS/$PATH_POLL_RESULTS_RESULT"

    override fun getType(uri: Uri) = GamePollResultsResult.CONTENT_TYPE

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        return SelectionBuilder()
            .table(Tables.POLLS_RESULTS_RESULT_JOIN_POLLS_RESULTS_JOIN_POLLS)
            .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS_RESULT)
            .whereEquals(GamePolls.Columns.GAME_ID, gameId)
            .whereEquals(GamePolls.Columns.POLL_NAME, pollName)
    }
}
