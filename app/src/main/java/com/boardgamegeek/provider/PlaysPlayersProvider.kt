package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYERS
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.Companion.QUERY_KEY_GROUP_BY
import com.boardgamegeek.provider.BggContract.Companion.QUERY_VALUE_COLOR
import com.boardgamegeek.provider.BggContract.Companion.QUERY_VALUE_NAME_NOT_USER
import com.boardgamegeek.provider.BggContract.Companion.QUERY_VALUE_PLAY
import com.boardgamegeek.provider.BggContract.Companion.QUERY_VALUE_UNIQUE_NAME
import com.boardgamegeek.provider.BggContract.Companion.QUERY_VALUE_UNIQUE_PLAYER
import com.boardgamegeek.provider.BggContract.Companion.QUERY_VALUE_UNIQUE_USER
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class PlaysPlayersProvider : BaseProvider() {

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder = SelectionBuilder().table(Tables.PLAY_PLAYERS)

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val builder = when (uri.getQueryParameter(QUERY_KEY_GROUP_BY).orEmpty()) {
            QUERY_VALUE_NAME_NOT_USER -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS)
                .whereEqualsOrNull(PlayPlayers.Columns.USER_NAME, "")
                .groupBy(PlayPlayers.Columns.NAME)
            QUERY_VALUE_UNIQUE_NAME -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS)
                .mapToTable(BaseColumns._ID, Tables.PLAY_PLAYERS)
                .mapAsSum(Plays.Columns.SUM_QUANTITY, Plays.Columns.QUANTITY)
                .map(Plays.Columns.SUM_WINS, "SUM(CASE WHEN ${PlayPlayers.Columns.WIN}=1 THEN ${Plays.Columns.QUANTITY} ELSE 0 END)")
                .where("${PlayPlayers.Columns.NAME}!= '' OR ${PlayPlayers.Columns.USER_NAME}!=''")
                .groupBy(PlayPlayers.Columns.UNIQUE_NAME)
            QUERY_VALUE_UNIQUE_PLAYER -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS)
                .mapToTable(BaseColumns._ID, Tables.PLAY_PLAYERS)
                .mapAsSum(Plays.Columns.SUM_QUANTITY, Plays.Columns.QUANTITY)
                .map(Plays.Columns.SUM_WINS, "SUM(CASE WHEN ${PlayPlayers.Columns.WIN}=1 THEN ${Plays.Columns.QUANTITY} ELSE 0 END)")
                .where("${PlayPlayers.Columns.NAME}!= '' OR ${PlayPlayers.Columns.USER_NAME}!=''")
                .groupBy("${PlayPlayers.Columns.NAME},${PlayPlayers.Columns.USER_NAME}")
            QUERY_VALUE_UNIQUE_USER -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS)
                .mapToTable(BaseColumns._ID, Tables.PLAY_PLAYERS)
                .mapAsSum(Plays.Columns.SUM_QUANTITY, Plays.Columns.QUANTITY)
                .map(Plays.Columns.SUM_WINS, "SUM(CASE WHEN ${PlayPlayers.Columns.WIN}=1 THEN ${Plays.Columns.QUANTITY} ELSE 0 END)")
                .where("${PlayPlayers.Columns.USER_NAME}!=''")
                .groupBy(PlayPlayers.Columns.USER_NAME)
            QUERY_VALUE_COLOR -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES)
                .mapToTable(BaseColumns._ID, Tables.PLAY_PLAYERS)
                .groupBy(PlayPlayers.Columns.COLOR)
            QUERY_VALUE_PLAY -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES)
                .mapToTable(BaseColumns._ID, Tables.PLAYS)
                .groupBy(Plays.Columns.PLAY_ID)
            else -> SelectionBuilder()
                .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES)
                .mapToTable(BaseColumns._ID, Tables.PLAY_PLAYERS)
                .mapToTable(PlayPlayers.Columns.NAME, Tables.PLAY_PLAYERS)
        }
        builder
            .mapAsCount(PlayPlayers.Columns.COUNT)
            .mapToTable(Plays.Columns.SYNC_TIMESTAMP, Tables.PLAYS)
            .map(PlayPlayers.Columns.UNIQUE_NAME, "IFNULL(NULLIF(user_name,''), name)")
            .map(PlayPlayers.Columns.DESCRIPTION, "name || IFNULL(NULLIF(' ('||user_name||')', ' ()'), '')")
        return builder
    }

    override val defaultSortOrder = "${Plays.Columns.DATE} DESC, ${PlayPlayers.DEFAULT_SORT}"

    override val path = "$PATH_PLAYS/$PATH_PLAYERS"

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        val internalPlayId = values.getAsLong(PlayPlayers.Columns._PLAY_ID) ?: INVALID_ID.toLong()
        val internalPlayerId = db.insertOrThrow(Tables.PLAY_PLAYERS, null, values)
        return Plays.buildPlayerUri(internalPlayId, internalPlayerId)
    }
}
