package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamePollResultsColumns.POLL_ID
import com.boardgamegeek.provider.BggContract.GamePollResultsColumns.POLL_RESULTS_KEY
import com.boardgamegeek.provider.BggContract.GamePollResultsResultColumns.POLL_RESULTS_ID
import com.boardgamegeek.provider.BggContract.GamePollsColumns.POLL_NAME
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPollsNameResultsKeyResultKeyProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePollResultsResult.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS/*/$PATH_POLL_RESULTS_RESULT/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val key = Games.getPollResultsKey(uri)
        val key2 = Games.getPollResultsResultKey(uri)
        return SelectionBuilder()
                .table(Tables.GAME_POLL_RESULTS_RESULT)
                .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
                .where("$POLL_RESULTS_ID = (SELECT ${Tables.GAME_POLL_RESULTS}.$_ID FROM ${Tables.GAME_POLL_RESULTS} WHERE ${Tables.GAME_POLL_RESULTS}.$POLL_RESULTS_KEY=? AND ${Tables.GAME_POLL_RESULTS}.$POLL_ID = (SELECT ${Tables.GAME_POLLS}.$_ID FROM ${Tables.GAME_POLLS} WHERE $GAME_ID=? AND $POLL_NAME=?))",
                        key, gameId.toString(), pollName)
                .whereEquals(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, key2)
    }
}
