package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlayersNameColorsProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayerColors.CONTENT_TYPE

    override val path = "$PATH_PLAYERS/*/$PATH_COLORS"

    override val defaultSortOrder = PlayerColors.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val playerName = PlayerColors.getPlayerName(uri)
        return SelectionBuilder().table(Tables.PLAYER_COLORS)
                .where("${PlayerColors.PLAYER_TYPE}=?", PlayerColors.TYPE_PLAYER.toString())
                .where("${PlayerColors.PLAYER_NAME}=?", playerName)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        val playerName = PlayerColors.getPlayerName(uri)
        if (playerName.isNullOrBlank()) {
            throw SQLException("Missing player name.")
        }
        values.put(PlayerColors.PLAYER_TYPE, PlayerColors.TYPE_PLAYER)
        values.put(PlayerColors.PLAYER_NAME, playerName)
        db.insertOrThrow(Tables.PLAYER_COLORS, null, values)
        return PlayerColors.buildUserUri(playerName, values.getAsInteger(PlayerColors.PLAYER_COLOR_SORT_ORDER))
    }
}
