package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamePollResultsColumns.POLL_ID
import com.boardgamegeek.provider.BggContract.GamePollResultsColumns.POLL_RESULTS_PLAYERS
import com.boardgamegeek.provider.BggContract.GamePollsColumns.POLL_NAME
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPollsNameResultsKeyProvider : BaseProvider() {
    override val path = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS/*"

    override fun getType(uri: Uri) = GamePollResults.CONTENT_ITEM_TYPE

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val key = Games.getPollResultsKey(uri)
        return SelectionBuilder()
                .table(Tables.GAME_POLL_RESULTS)
                .mapToTable(_ID, Tables.GAME_POLL_RESULTS)
                .where("$POLL_ID = (SELECT ${Tables.GAME_POLLS}.$_ID FROM ${Tables.GAME_POLLS} WHERE $GAME_ID=? AND $POLL_NAME=?)", gameId.toString(), pollName)
                .whereEquals(POLL_RESULTS_PLAYERS, key)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val players = Games.getPollResultsKey(uri)
        return SelectionBuilder().table(Tables.POLLS_JOIN_POLL_RESULTS)
                .mapToTable(_ID, Tables.GAME_POLL_RESULTS)
                .whereEquals(GAME_ID, gameId)
                .whereEquals(POLL_NAME, pollName)
                .whereEquals(POLL_RESULTS_PLAYERS, players)
    }
}
