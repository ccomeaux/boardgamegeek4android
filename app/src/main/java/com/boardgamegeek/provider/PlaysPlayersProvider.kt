package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*

import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlaysPlayersProvider : BaseProvider() {
    override val path = "$PATH_PLAYS/$PATH_PLAYERS"

    override val defaultSortOrder = "${Plays.DATE} DESC, ${PlayPlayers.DEFAULT_SORT}"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder = SelectionBuilder().table(Tables.PLAY_PLAYERS)

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val builder: SelectionBuilder
        when (uri.getQueryParameter(QUERY_KEY_GROUP_BY) ?: "") {
            QUERY_VALUE_NAME_NOT_USER -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS)
                    .whereEqualsOrNull(PlayPlayers.USER_NAME, "")
                    .groupBy(PlayPlayers.NAME)
            QUERY_VALUE_UNIQUE_NAME -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS)
                    .mapToTable(Plays._ID, Tables.PLAY_PLAYERS)
                    .mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY)
                    .map(Plays.SUM_WINS, "SUM(CASE WHEN ${PlayPlayers.WIN}=1 THEN ${Plays.QUANTITY} ELSE 0 END)")
                    .where("${PlayPlayers.NAME}!= '' OR ${PlayPlayers.USER_NAME}!=''")
                    .groupBy(PlayPlayers.UNIQUE_NAME)
            QUERY_VALUE_UNIQUE_PLAYER -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS)
                    .mapToTable(Plays._ID, Tables.PLAY_PLAYERS)
                    .mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY)
                    .map(Plays.SUM_WINS, "SUM(CASE WHEN ${PlayPlayers.WIN}=1 THEN ${Plays.QUANTITY} ELSE 0 END)")
                    .where("${PlayPlayers.NAME}!= '' OR ${PlayPlayers.USER_NAME}!=''")
                    .groupBy("${PlayPlayers.NAME},${PlayPlayers.USER_NAME}")
            QUERY_VALUE_UNIQUE_USER -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS)
                    .mapToTable(Plays._ID, Tables.PLAY_PLAYERS)
                    .mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY)
                    .map(Plays.SUM_WINS, "SUM(CASE WHEN ${PlayPlayers.WIN}=1 THEN ${Plays.QUANTITY} ELSE 0 END)")
                    .where("${PlayPlayers.USER_NAME}!=''")
                    .groupBy(PlayPlayers.USER_NAME)
            QUERY_VALUE_COLOR -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES)
                    .mapToTable(Plays._ID, Tables.PLAY_PLAYERS)
                    .groupBy(PlayPlayers.COLOR)
            QUERY_VALUE_PLAY -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES)
                    .mapToTable(Plays._ID, Tables.PLAYS)
                    .groupBy(Plays.PLAY_ID)
            else -> builder = SelectionBuilder()
                    .table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES)
                    .mapToTable(PlayPlayers._ID, Tables.PLAY_PLAYERS)
                    .mapToTable(PlayPlayers.NAME, Tables.PLAY_PLAYERS)
        }
        builder
                .mapAsCount(PlayPlayers.COUNT)
                .mapToTable(Plays.SYNC_TIMESTAMP, Tables.PLAYS)
                .map(PlayPlayers.UNIQUE_NAME, "IFNULL(NULLIF(user_name,''), name)")
                .map(PlayPlayers.DESCRIPTION, "name || IFNULL(NULLIF(' ('||user_name||')', ' ()'), '')")
        return builder
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        val internalPlayId = values.getAsLong(PlayPlayers._PLAY_ID) ?: INVALID_ID.toLong()
        val internalPlayerId = db.insertOrThrow(Tables.PLAY_PLAYERS, null, values)
        return Plays.buildPlayerUri(internalPlayId, internalPlayerId)
    }
}
