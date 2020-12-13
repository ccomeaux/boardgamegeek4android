package com.boardgamegeek.export

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import com.boardgamegeek.export.model.User
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.extensions.rowExists
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.google.gson.Gson
import com.google.gson.stream.JsonReader

class UserImportTask(context: Context, uri: Uri) : JsonImportTask<User>(context, Constants.TYPE_USERS, Constants.TYPE_USERS_DESCRIPTION, uri) {
    override fun parseItem(gson: Gson, reader: JsonReader): User {
        return gson.fromJson(reader, User::class.java)
    }

    override fun importRecord(item: User, version: Int) {
        if (context.contentResolver.rowExists(BggContract.Buddies.buildBuddyUri(item.name))) {
            val batch = arrayListOf<ContentProviderOperation>()
            item.colors.filter { it.color.isNotBlank() }.forEach { color ->
                val builder = if (context.contentResolver.rowExists(PlayerColors.buildUserUri(item.name, color.sort))) {
                    ContentProviderOperation
                            .newUpdate(PlayerColors.buildUserUri(item.name, color.sort))
                } else {
                    ContentProviderOperation
                            .newInsert(PlayerColors.buildUserUri(item.name))
                            .withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, color.sort)
                }
                batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, color.color).build())
            }
            context.contentResolver.applyBatch(batch)
        }
    }
}