package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.DataUtils
import com.boardgamegeek.util.SelectionBuilder
import timber.log.Timber

class GamesIdPollsNameResultsKeyResultProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePollResultsResult.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_POLLS/*/$PATH_POLL_RESULTS/*/$PATH_POLL_RESULTS_RESULT"

    override val defaultSortOrder = GamePollResultsResult.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val key = Games.getPollResultsKey(uri)
        return SelectionBuilder()
                .table(Tables.GAME_POLL_RESULTS_RESULT)
                .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
                .whereEquals(GamePollResults.POLL_RESULTS_KEY, key)
                .where(
                        "game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id =(SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)", gameId.toString(), pollName)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val players = Games.getPollResultsKey(uri)
        return SelectionBuilder()
                .table(Tables.POLL_RESULTS_JOIN_POLL_RESULTS_RESULT)
                .mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS_RESULT)
                .whereEquals(GamePollResults.POLL_RESULTS_PLAYERS, players)
                .where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)", gameId.toString(), pollName)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        val players = Games.getPollResultsKey(uri)
        val builder = GamesIdPollsNameResultsKeyProvider().buildSimpleSelection(Games
                .buildPollResultsUri(gameId, pollName, players))
        val id = queryInt(db, builder, GamePollResultsResult._ID)
        values.put(GamePollResultsResult.POLL_RESULTS_ID, id)
        val key = DataUtils.generatePollResultsKey(
                values.getAsString(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL),
                values.getAsString(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE))
        values.put(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, key)
        try {
            val rowId = db.insertOrThrow(Tables.GAME_POLL_RESULTS_RESULT, null, values)
            if (rowId != -1L) {
                val key2 = values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS)
                return Games.buildPollResultsResultUri(gameId, pollName, players, key2)
            }
        } catch (e: SQLException) {
            Timber.e(e, "Problem inserting poll %2\$s %3\$s for game %1\$s", gameId, pollName, players)
            notifyException(context, e)
        }
        return null
    }
}