package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.export.model.User
import com.boardgamegeek.provider.BggContract
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class UserExportTask(context: Context, uri: Uri) : JsonExportTask<User>(context, Constants.TYPE_USERS, uri) {
    override val version: Int
        get() = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                BggContract.Buddies.CONTENT_URI,
                User.PROJECTION,
                null, null, null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        val user = User.fromCursor(cursor)
        user.addColors(context)
        if (user.hasColors()) {
            gson.toJson(user, User::class.java, writer)
        }
    }
}