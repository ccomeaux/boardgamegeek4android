package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamePollResultsColumns.POLL_ID
import com.boardgamegeek.provider.BggContract.GamePollsColumns.POLL_NAME
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder
import timber.log.Timber

class GamesIdPollsNameResultsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePollResults.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS"

    override val defaultSortOrder = GamePollResults.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        return SelectionBuilder()
                .table(Tables.GAME_POLL_RESULTS)
                .mapToTable(_ID, Tables.GAME_POLL_RESULTS)
                .where("$POLL_ID = (SELECT ${Tables.GAME_POLLS}.$_ID FROM ${Tables.GAME_POLLS} WHERE $GAME_ID=? AND $POLL_NAME=?)", gameId.toString(), pollName)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        return SelectionBuilder().table(Tables.POLLS_JOIN_POLL_RESULTS)
                .mapToTable(_ID, Tables.GAME_POLL_RESULTS)
                .whereEquals(GamePolls.GAME_ID, gameId)
                .whereEquals(GamePolls.POLL_NAME, pollName)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val builder = GamesIdPollsNameProvider().buildSimpleSelection(Games.buildPollsUri(gameId, pollName))
        val pollId = queryInt(db, builder, GamePolls._ID)
        values.put(GamePollResults.POLL_ID, pollId)
        var key = values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS)
        if (key.isNullOrEmpty()) key =  "X"
        values.put(GamePollResults.POLL_RESULTS_KEY, key)
        try {
            val rowId = db.insertOrThrow(Tables.GAME_POLL_RESULTS, null, values)
            if (rowId != -1L) {
                return Games.buildPollResultsUri(gameId, pollName, values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS))
            }
        } catch (e: SQLException) {
            Timber.e(e, "Problem inserting poll %2\$s for game %1\$s", gameId, pollName)
            notifyException(context, e)
        }
        return null
    }
}
