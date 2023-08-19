package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.db.model.UserLocal
import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Avatars
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.Companion.COLLATE_NOCASE
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
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
            Buddies.buildBuddyUri(username),
            projection(),
        ) {
            fromCursor(it)
        }
    }

    suspend fun loadUsers(sortBy: UsersSortBy = UsersSortBy.USERNAME, buddiesOnly: Boolean): List<UserLocal> =
        withContext(Dispatchers.IO) {
            val sortOrder = when (sortBy) {
                UsersSortBy.USERNAME -> Buddies.Columns.BUDDY_NAME
                UsersSortBy.FIRST_NAME -> Buddies.Columns.BUDDY_FIRSTNAME
                UsersSortBy.LAST_NAME -> Buddies.Columns.BUDDY_LASTNAME
                UsersSortBy.UPDATED -> Buddies.Columns.UPDATED
            }.plus(" $COLLATE_NOCASE ASC")
            context.contentResolver.loadList(
                Buddies.CONTENT_URI,
                projection(),
                if (buddiesOnly) "${Buddies.Columns.BUDDY_ID}!=? AND ${Buddies.Columns.BUDDY_FLAG}=1" else null,
                if (buddiesOnly) arrayOf(Authenticator.getUserId(context)) else null,
                sortOrder
            ) {
                fromCursor(it)
            }
        }

    private fun projection() = arrayOf(
        BaseColumns._ID,
        Buddies.Columns.BUDDY_ID,
        Buddies.Columns.BUDDY_NAME,
        Buddies.Columns.BUDDY_FIRSTNAME,
        Buddies.Columns.BUDDY_LASTNAME,
        Buddies.Columns.AVATAR_URL,
        Buddies.Columns.PLAY_NICKNAME,
        Buddies.Columns.UPDATED,
        Buddies.Columns.UPDATED_LIST,
        Buddies.Columns.BUDDY_FLAG,
        Buddies.Columns.SYNC_HASH_CODE,
    )

    private fun fromCursor(it: Cursor) = UserLocal(
        internalId = it.getInt(0),
        buddyId = it.getInt(1),
        buddyName = it.getString(2),
        buddyFirstName = it.getStringOrNull(3),
        buddyLastName = it.getStringOrNull(4),
        avatarUrl = it.getStringOrNull(5),
        playNickname = it.getStringOrNull(6),
        updatedTimestamp = it.getLongOrNull(7),
        updatedListTimestamp = it.getLong(8),
        buddyFlag = it.getBooleanOrNull(9),
        syncHashCode = it.getInt(10),
    )

    suspend fun saveUser(user: UserForUpsert): Boolean = withContext(Dispatchers.IO) {
        if (user.buddyName.isNotBlank()) {
            val values = contentValuesOf(Buddies.Columns.UPDATED to user.updatedTimestamp)
            val oldSyncHashCode = context.contentResolver.queryInt(Buddies.buildBuddyUri(user.buddyName), Buddies.Columns.SYNC_HASH_CODE)
            val newSyncHashCode = user.generateSyncHashCode()
            if (oldSyncHashCode != newSyncHashCode) {
                values.put(Buddies.Columns.BUDDY_ID, user.buddyId)
                values.put(Buddies.Columns.BUDDY_NAME, user.buddyName)
                values.put(Buddies.Columns.BUDDY_FIRSTNAME, user.buddyFirstName)
                values.put(Buddies.Columns.BUDDY_LASTNAME, user.buddyLastName)
                values.put(Buddies.Columns.AVATAR_URL, user.avatarUrl)
                values.put(Buddies.Columns.SYNC_HASH_CODE, newSyncHashCode)
            }
            upsert(values, user.buddyName)
        } else false
    }

    suspend fun saveBuddy(buddy: UserAsBuddyForUpsert): Boolean = withContext(Dispatchers.IO) {
        if (buddy.buddyId != INVALID_ID && buddy.userName.isNotBlank()) {
            val values = contentValuesOf(
                Buddies.Columns.BUDDY_ID to buddy.buddyId,
                Buddies.Columns.BUDDY_NAME to buddy.userName,
                Buddies.Columns.BUDDY_FLAG to true,
                Buddies.Columns.UPDATED_LIST to buddy.updatedTimestamp
            )
            upsert(values, buddy.userName)
        } else {
            Timber.i("Un-savable buddy %s (%d)", buddy.userName, buddy.buddyId)
            false
        }
    }

    suspend fun updateNickname(username: String, nickName: String) = withContext(Dispatchers.IO) {
        if (username.isNotBlank()) {
            upsert(contentValuesOf(Buddies.Columns.PLAY_NICKNAME to nickName), username)
        } else false
    }

    suspend fun upsert(values: ContentValues, username: String): Boolean = withContext(Dispatchers.IO) {
        val uri = Buddies.buildBuddyUri(username)
        val internalId = context.contentResolver.queryInt(uri, BaseColumns._ID, INVALID_ID)
        if (internalId != INVALID_ID) {
            values.remove(Buddies.Columns.BUDDY_NAME)
            val count = context.contentResolver.update(uri, values, null, null)
            Timber.d("Updated %,d user rows at %s", count, uri)
            maybeDeleteAvatar(values, uri)
            count == 1
        } else {
            values.put(Buddies.Columns.BUDDY_NAME, username)
            val insertedUri = context.contentResolver.insert(Buddies.CONTENT_URI, values)
            Timber.d("Inserted user at %s", insertedUri)
            username == insertedUri?.lastPathSegment
        }
    }

    private suspend fun maybeDeleteAvatar(values: ContentValues, uri: Uri) = withContext(Dispatchers.IO) {
        if (values.containsKey(Buddies.Columns.AVATAR_URL)) {
            val newAvatarUrl: String = values.getAsString(Buddies.Columns.AVATAR_URL).orEmpty()
            val oldAvatarUrl = context.contentResolver.queryString(uri, Buddies.Columns.AVATAR_URL)
            if (newAvatarUrl != oldAvatarUrl) {
                val avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl)
                if (avatarFileName.isNotBlank()) {
                    context.contentResolver.delete(Avatars.buildUri(avatarFileName), null, null)
                }
            }
        }
    }

    suspend fun deleteUsers(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Buddies.CONTENT_URI, null, null)
    }

    suspend fun deleteBuddiesAsOf(updateTimestamp: Long): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(
            Buddies.CONTENT_URI,
            "${Buddies.Columns.BUDDY_FLAG}=1 AND ${Buddies.Columns.UPDATED_LIST}<?",
            arrayOf(updateTimestamp.toString())
        )
    }

    suspend fun updateColors(username: String, colors: List<Pair<Int, String>>) = withContext(Dispatchers.IO) {
        if (context.contentResolver.rowExists(Buddies.buildBuddyUri(username))) {
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
