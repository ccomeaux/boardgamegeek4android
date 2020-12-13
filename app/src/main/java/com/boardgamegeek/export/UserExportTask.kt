package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.export.model.PlayerColor
import com.boardgamegeek.export.model.User
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class UserExportTask(context: Context, uri: Uri) : JsonExportTask<User>(context, Constants.TYPE_USERS, Constants.TYPE_USERS_DESCRIPTION, uri) {
    override val version: Int
        get() = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                BggContract.Buddies.CONTENT_URI,
                arrayOf(BggContract.Buddies.BUDDY_NAME),
                null, null, null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        val name = cursor.getString(0)

        val colors = mutableListOf<PlayerColor>()
        context.contentResolver.load(
                PlayerColors.buildUserUri(name),
                arrayOf(
                        PlayerColors._ID,
                        PlayerColors.PLAYER_COLOR_SORT_ORDER,
                        PlayerColors.PLAYER_COLOR
                )
        )?.use {
            while (it.moveToNext()) {
                it.getString(2).orEmpty().also { color ->
                    if (color.isNotBlank()) colors.add(PlayerColor(it.getInt(1), color))
                }
            }
        }

        if (colors.isNotEmpty()) {
            gson.toJson(User(name, colors), User::class.java, writer)
        }
    }
}