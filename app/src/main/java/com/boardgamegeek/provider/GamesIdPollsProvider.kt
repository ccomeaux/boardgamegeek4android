package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamePollsColumns.POLL_NAME
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder
import timber.log.Timber

class GamesIdPollsProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePolls.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_POLLS"

    override val defaultSortOrder = GamePolls.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAME_POLLS).whereEquals(GAME_ID, gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val gameId = Games.getGameId(uri)
        values.put(GAME_ID, gameId)
        try {
            if (db.insertOrThrow(Tables.GAME_POLLS, null, values) != -1L) {
                return Games.buildPollsUri(gameId, values.getAsString(POLL_NAME))
            }
        } catch (e: SQLException) {
            Timber.e(e, "Problem inserting poll for game %1\$s", gameId)
            notifyException(context, e)
        }
        return null
    }
}
