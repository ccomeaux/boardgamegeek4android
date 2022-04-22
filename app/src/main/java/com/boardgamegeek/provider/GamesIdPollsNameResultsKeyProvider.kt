package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_POLLS
import com.boardgamegeek.provider.BggContract.Companion.PATH_POLL_RESULTS
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdPollsNameResultsKeyProvider : BaseProvider() {
    override val path = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS/*"

    override fun getType(uri: Uri) = GamePollResults.CONTENT_ITEM_TYPE

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val key = Games.getPollResultsKey(uri)
        return SelectionBuilder()
            .table(Tables.GAME_POLL_RESULTS)
            .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
            .where(
                "${GamePollResults.Columns.POLL_ID} = (SELECT ${Tables.GAME_POLLS}.${BaseColumns._ID} FROM ${Tables.GAME_POLLS} WHERE ${GamePolls.Columns.GAME_ID}=? AND ${GamePolls.Columns.POLL_NAME}=?)",
                gameId.toString(),
                pollName
            )
            .whereEquals(GamePollResults.Columns.POLL_RESULTS_PLAYERS, key)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val players = Games.getPollResultsKey(uri)
        return SelectionBuilder().table(Tables.POLLS_JOIN_POLL_RESULTS)
            .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
            .whereEquals(GamePolls.Columns.GAME_ID, gameId)
            .whereEquals(GamePolls.Columns.POLL_NAME, pollName)
            .whereEquals(GamePollResults.Columns.POLL_RESULTS_PLAYERS, players)
    }
}
