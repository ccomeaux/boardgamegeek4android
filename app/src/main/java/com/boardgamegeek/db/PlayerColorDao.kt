package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.Context
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.extensions.rowExists
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerColorDao(private val context: Context) {
    suspend fun updateColors(username: String, colors: List<Pair<Int, String>>) = withContext(Dispatchers.IO) {
        if (context.contentResolver.rowExists(BggContract.Users.buildUserUri(username))) {
            val batch = arrayListOf<ContentProviderOperation>()
            colors.filter { it.second.isNotBlank() }.forEach { (sort, color) ->
                val builder = if (context.contentResolver.rowExists(BggContract.PlayerColors.buildUserUri(username, sort))) {
                    ContentProviderOperation
                        .newUpdate(BggContract.PlayerColors.buildUserUri(username, sort))
                } else {
                    ContentProviderOperation
                        .newInsert(BggContract.PlayerColors.buildUserUri(username))
                        .withValue(BggContract.PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER, sort)
                }
                batch.add(builder.withValue(BggContract.PlayerColors.Columns.PLAYER_COLOR, color).build())
            }
            context.contentResolver.applyBatch(batch)
        }
    }
}