package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.BriefBuddyEntity
import com.boardgamegeek.entities.UserEntity
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

    suspend fun loadUser(username: String): UserEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.loadEntity(
            Buddies.buildBuddyUri(username),
            arrayOf(
                BaseColumns._ID,
                Buddies.Columns.BUDDY_ID,
                Buddies.Columns.BUDDY_NAME,
                Buddies.Columns.BUDDY_FIRSTNAME,
                Buddies.Columns.BUDDY_LASTNAME,
                Buddies.Columns.AVATAR_URL,
                Buddies.Columns.PLAY_NICKNAME,
                Buddies.Columns.UPDATED,
            )
        ) {
            UserEntity(
                internalId = it.getLong(0),
                id = it.getInt(1),
                userName = it.getStringOrNull(2).orEmpty(),
                firstName = it.getStringOrNull(3).orEmpty(),
                lastName = it.getStringOrNull(4).orEmpty(),
                avatarUrlRaw = it.getStringOrNull(5).orEmpty(),
                playNickname = it.getStringOrNull(6).orEmpty(),
                updatedTimestamp = it.getLongOrNull(7) ?: 0L
            )
        }
    }

    suspend fun loadUsers(sortBy: UsersSortBy = UsersSortBy.USERNAME, buddiesOnly: Boolean): List<UserEntity> =
        withContext(Dispatchers.IO) {
            val sortOrder = when (sortBy) {
                UsersSortBy.USERNAME -> Buddies.Columns.BUDDY_NAME
                UsersSortBy.FIRST_NAME -> Buddies.Columns.BUDDY_FIRSTNAME
                UsersSortBy.LAST_NAME -> Buddies.Columns.BUDDY_LASTNAME
                UsersSortBy.UPDATED -> Buddies.Columns.UPDATED
            }.plus(" $COLLATE_NOCASE ASC")
            context.contentResolver.loadList(
                Buddies.CONTENT_URI,
                arrayOf(
                    BaseColumns._ID,
                    Buddies.Columns.BUDDY_ID,
                    Buddies.Columns.BUDDY_NAME,
                    Buddies.Columns.BUDDY_FIRSTNAME,
                    Buddies.Columns.BUDDY_LASTNAME,
                    Buddies.Columns.AVATAR_URL,
                    Buddies.Columns.PLAY_NICKNAME,
                    Buddies.Columns.UPDATED
                ),
                if (buddiesOnly) "${Buddies.Columns.BUDDY_ID}!=? AND ${Buddies.Columns.BUDDY_FLAG}=1" else null,
                if (buddiesOnly) arrayOf(Authenticator.getUserId(context)) else null,
                sortOrder
            ) {
                UserEntity(
                    internalId = it.getLong(0),
                    id = it.getInt(1),
                    userName = it.getStringOrNull(2).orEmpty(),
                    firstName = it.getStringOrNull(3).orEmpty(),
                    lastName = it.getStringOrNull(4).orEmpty(),
                    avatarUrlRaw = it.getStringOrNull(5).orEmpty(),
                    playNickname = it.getStringOrNull(6).orEmpty(),
                    updatedTimestamp = it.getLongOrNull(7) ?: 0L,
                )
            }
        }

    suspend fun saveUser(user: UserEntity, updateTime: Long = System.currentTimeMillis()): UserEntity =
        withContext(Dispatchers.IO) {
            if (user.userName.isNotBlank()) {
                val values = contentValuesOf(
                    Buddies.Columns.UPDATED to updateTime,
                    Buddies.Columns.UPDATED_LIST to updateTime
                )
                val oldSyncHashCode = context.contentResolver.queryInt(Buddies.buildBuddyUri(user.userName), Buddies.Columns.SYNC_HASH_CODE)
                val newSyncHashCode = user.generateSyncHashCode()
                if (oldSyncHashCode != newSyncHashCode) {
                    values.put(Buddies.Columns.BUDDY_ID, user.id)
                    values.put(Buddies.Columns.BUDDY_NAME, user.userName)
                    values.put(Buddies.Columns.BUDDY_FIRSTNAME, user.firstName)
                    values.put(Buddies.Columns.BUDDY_LASTNAME, user.lastName)
                    values.put(Buddies.Columns.AVATAR_URL, user.avatarUrl)
                    values.put(Buddies.Columns.SYNC_HASH_CODE, newSyncHashCode)
                }
                val internalId = upsert(values, user.userName, user.id)
                user.copy(internalId = internalId, updatedTimestamp = updateTime)
            } else user
        }

    suspend fun saveBuddy(buddy: BriefBuddyEntity) = withContext(Dispatchers.IO) {
        if (buddy.id != INVALID_ID && buddy.userName.isNotBlank()) {
            val values = contentValuesOf(
                Buddies.Columns.BUDDY_ID to buddy.id,
                Buddies.Columns.BUDDY_NAME to buddy.userName,
                Buddies.Columns.BUDDY_FLAG to true,
                Buddies.Columns.UPDATED_LIST to buddy.updatedTimestamp
            )
            upsert(values, buddy.userName, buddy.id)
        } else {
            Timber.i("Un-savable buddy %s (%d)", buddy.userName, buddy.id)
        }
    }

    suspend fun upsert(values: ContentValues, username: String, userId: Int? = null): Long = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Buddies.buildBuddyUri(username)
        val internalId = resolver.queryLong(uri, BaseColumns._ID, INVALID_ID.toLong())
        if (internalId != INVALID_ID.toLong()) {
            values.remove(Buddies.Columns.BUDDY_NAME)
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d buddy rows at %s", count, uri)
            maybeDeleteAvatar(values, uri)
            internalId
        } else {
            values.put(Buddies.Columns.BUDDY_NAME, username)
            userId?.let { values.put(Buddies.Columns.BUDDY_ID, it) }
            val insertedUri = resolver.insert(Buddies.CONTENT_URI, values)
            Timber.d("Inserted buddy at %s", insertedUri)
            insertedUri?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
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

    suspend fun deleteUsersAsOf(updateTimestamp: Long): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(
            Buddies.CONTENT_URI,
            "${Buddies.Columns.UPDATED_LIST}<?",
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
