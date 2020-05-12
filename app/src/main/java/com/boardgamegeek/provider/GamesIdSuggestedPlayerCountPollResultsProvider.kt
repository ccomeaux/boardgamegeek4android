package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollResultsColumns.PLAYER_COUNT
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder
import timber.log.Timber

class GamesIdSuggestedPlayerCountPollResultsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameSuggestedPlayerCountPollPollResults.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS"

    override val defaultSortOrder = GameSuggestedPlayerCountPollPollResults.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
                .whereEquals(GAME_ID, gameId)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.POLLS_JOIN_GAMES)
                .whereEquals("${Tables.GAMES}.$GAME_ID", gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val gameId = Games.getGameId(uri)
        values.put(GamePolls.GAME_ID, gameId)
        try {
            val roeId = db.insertOrThrow(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS, null, values)
            if (roeId != -1L) {
                return Games.buildSuggestedPlayerCountPollResultsUri(gameId, values.getAsString(PLAYER_COUNT))
            }
        } catch (e: SQLException) {
            Timber.e(e, "Problem inserting poll result for game %s", gameId)
            notifyException(context, e)
        }
        return null
    }
}
