package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.db.model.UserLocal
import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Avatars
import com.boardgamegeek.provider.BggContract.Users
import com.boardgamegeek.provider.BggContract.Companion.COLLATE_NOCASE
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserDao(private val context: Context) {
    enum class UsersSortBy {
        FIRST_NAME, LAST_NAME, USERNAME, UPDATED
    }

    suspend fun loadUser(username: String): UserLocal? = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext null
        context.contentResolver.loadEntity(
            Users.buildUserUri(username),
            projection(),
        ) {
            fromCursor(it)
        }
    }

    suspend fun loadUsers(sortBy: UsersSortBy = UsersSortBy.USERNAME, buddiesOnly: Boolean): List<UserLocal> =
        withContext(Dispatchers.IO) {
            val sortOrder = when (sortBy) {
                UsersSortBy.USERNAME -> Users.Columns.USERNAME
                UsersSortBy.FIRST_NAME -> Users.Columns.FIRST_NAME
                UsersSortBy.LAST_NAME -> Users.Columns.LAST_NAME
                UsersSortBy.UPDATED -> Users.Columns.UPDATED_DETAIL_TIMESTAMP
            }.plus(" $COLLATE_NOCASE ASC")
            context.contentResolver.loadList(
                Users.CONTENT_URI,
                projection(),
                if (buddiesOnly) "${Users.Columns.BUDDY_FLAG}=1" else null,
                null,
                sortOrder
            ) {
                fromCursor(it)
            }
        }

    private fun projection() = arrayOf(
        Users.Columns.USERNAME,
        Users.Columns.FIRST_NAME,
        Users.Columns.LAST_NAME,
        Users.Columns.AVATAR_URL,
        Users.Columns.PLAY_NICKNAME,
        Users.Columns.UPDATED_DETAIL_TIMESTAMP,
        Users.Columns.UPDATED_LIST_TIMESTAMP,
        Users.Columns.BUDDY_FLAG,
        Users.Columns.SYNC_HASH_CODE,
    )

    private fun fromCursor(it: Cursor) = UserLocal(
        username = it.getString(0),
        firstName = it.getStringOrNull(1),
        lastName = it.getStringOrNull(2),
        avatarUrl = it.getStringOrNull(3),
        playNickname = it.getStringOrNull(4),
        updatedDetailTimestamp = it.getLongOrNull(5),
        updatedListTimestamp = it.getLong(6),
        buddyFlag = it.getBooleanOrNull(7),
        syncHashCode = it.getInt(8),
    )

    suspend fun saveUser(user: UserForUpsert): Boolean = withContext(Dispatchers.IO) {
        if (user.username.isNotBlank()) {
            val values = contentValuesOf(Users.Columns.UPDATED_DETAIL_TIMESTAMP to user.updatedDetailTimestamp)
            val oldSyncHashCode = context.contentResolver.queryInt(Users.buildUserUri(user.username), Users.Columns.SYNC_HASH_CODE)
            val newSyncHashCode = user.generateSyncHashCode()
            if (oldSyncHashCode != newSyncHashCode) {
                values.put(Users.Columns.USERNAME, user.username)
                values.put(Users.Columns.FIRST_NAME, user.firstName)
                values.put(Users.Columns.LAST_NAME, user.lastName)
                values.put(Users.Columns.AVATAR_URL, user.avatarUrl)
                values.put(Users.Columns.SYNC_HASH_CODE, newSyncHashCode)
            }
            upsert(values, user.username)
        } else false
    }

    suspend fun saveBuddy(buddy: UserAsBuddyForUpsert): Boolean = withContext(Dispatchers.IO) {
        if (buddy.username.isNotBlank()) {
            val values = contentValuesOf(
                Users.Columns.USERNAME to buddy.username,
                Users.Columns.BUDDY_FLAG to true,
                Users.Columns.UPDATED_LIST_TIMESTAMP to buddy.updatedTimestamp
            )
            upsert(values, buddy.username)
        } else {
            Timber.i("Un-savable buddy %s", buddy.username)
            false
        }
    }

    suspend fun updateNickname(username: String, nickName: String) = withContext(Dispatchers.IO) {
        if (username.isNotBlank()) {
            upsert(contentValuesOf(Users.Columns.PLAY_NICKNAME to nickName), username)
        } else false
    }

    suspend fun upsert(values: ContentValues, username: String): Boolean = withContext(Dispatchers.IO) {
        val uri = Users.buildUserUri(username)
        if (context.contentResolver.getCount(uri, Users.Columns.USERNAME) > 0) {
            values.remove(Users.Columns.USERNAME)
            val count = context.contentResolver.update(uri, values, null, null)
            Timber.d("Updated %,d user rows at %s", count, uri)
            maybeDeleteAvatar(values, uri)
            count == 1
        } else {
            values.put(Users.Columns.USERNAME, username)
            if (!values.containsKey(Users.Columns.UPDATED_LIST_TIMESTAMP)) {
                values.put(Users.Columns.UPDATED_LIST_TIMESTAMP, System.currentTimeMillis())
            }
            val insertedUri = context.contentResolver.insert(Users.CONTENT_URI, values)
            Timber.d("Inserted user at %s", insertedUri)
            username == insertedUri?.lastPathSegment
        }
    }

    private suspend fun maybeDeleteAvatar(values: ContentValues, uri: Uri) = withContext(Dispatchers.IO) {
        if (values.containsKey(Users.Columns.AVATAR_URL)) {
            val newAvatarUrl: String = values.getAsString(Users.Columns.AVATAR_URL).orEmpty()
            val oldAvatarUrl = context.contentResolver.queryString(uri, Users.Columns.AVATAR_URL)
            if (newAvatarUrl != oldAvatarUrl) {
                val avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl)
                if (avatarFileName.isNotBlank()) {
                    context.contentResolver.delete(Avatars.buildUri(avatarFileName), null, null)
                }
            }
        }
    }

    suspend fun deleteUsers(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Users.CONTENT_URI, null, null)
    }

    suspend fun deleteBuddiesAsOf(updateTimestamp: Long): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(
            Users.CONTENT_URI,
            "${Users.Columns.BUDDY_FLAG}=1 AND ${Users.Columns.UPDATED_LIST_TIMESTAMP}<?",
            arrayOf(updateTimestamp.toString())
        )
    }

    suspend fun updateColors(username: String, colors: List<Pair<Int, String>>) = withContext(Dispatchers.IO) {
        if (context.contentResolver.rowExists(Users.buildUserUri(username))) {
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
