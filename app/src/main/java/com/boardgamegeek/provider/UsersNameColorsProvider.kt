package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLORS
import com.boardgamegeek.provider.BggContract.Companion.PATH_USERS
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.boardgamegeek.provider.BggContract.PlayerColors.Columns.PLAYER_NAME
import com.boardgamegeek.provider.BggContract.PlayerColors.Columns.PLAYER_TYPE
import com.boardgamegeek.provider.BggDatabase.Tables

class UsersNameColorsProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayerColors.CONTENT_TYPE

    override val path = "$PATH_USERS/*/$PATH_COLORS"

    override val defaultSortOrder = PlayerColors.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder().table(Tables.PLAYER_COLORS)
            .where("$PLAYER_TYPE=?", PlayerColors.TYPE_USER.toString())
            .where("$PLAYER_NAME=?", PlayerColors.getUsername(uri))
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        val username = PlayerColors.getUsername(uri)
        if (username.isNullOrBlank()) {
            throw SQLException("Missing username.")
        }
        values.put(PLAYER_TYPE, PlayerColors.TYPE_USER)
        values.put(PLAYER_NAME, username)
        db.insertOrThrow(Tables.PLAYER_COLORS, null, values)
        return PlayerColors.buildUserUri(username, values.getAsInteger(PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER))
    }
}
