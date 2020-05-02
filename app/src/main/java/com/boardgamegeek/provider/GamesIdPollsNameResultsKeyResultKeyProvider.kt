package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPollsNameResultsKeyResultKeyProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePollResultsResult.CONTENT_ITEM_TYPE

    override fun getPath() = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS/*/$PATH_POLL_RESULTS_RESULT/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val key = Games.getPollResultsKey(uri)
        val key2 = Games.getPollResultsResultKey(uri)
        return SelectionBuilder()
                .table(Tables.GAME_POLL_RESULTS_RESULT)
                .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
                .where("pollresults_id = (SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.pollresults_key=? AND game_poll_results.poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?))",
                        key, gameId.toString(), pollName)
                .whereEquals(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, key2)
    }
}
