package com.boardgamegeek.db

import android.content.ContentValues
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.BriefBuddyEntity
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.model.User
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserDao(private val context: BggApplication) {
    enum class UsersSortBy {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    suspend fun loadUser(username: String): UserEntity? = withContext(Dispatchers.IO) {
        context.contentResolver.load(
            Buddies.buildBuddyUri(username),
            arrayOf(
                Buddies._ID,
                Buddies.BUDDY_ID,
                Buddies.BUDDY_NAME,
                Buddies.BUDDY_FIRSTNAME,
                Buddies.BUDDY_LASTNAME,
                Buddies.AVATAR_URL,
                Buddies.PLAY_NICKNAME,
                Buddies.UPDATED
            )
        )?.use {
            if (it.moveToFirst()) {
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
            } else null
        }
    }

    suspend fun loadBuddies(sortBy: UsersSortBy = UsersSortBy.USERNAME): List<UserEntity> =
        withContext(Dispatchers.IO) {
            val results = arrayListOf<UserEntity>()
            val sortOrder = when (sortBy) {
                UsersSortBy.USERNAME -> Buddies.BUDDY_NAME
                UsersSortBy.FIRST_NAME -> Buddies.BUDDY_FIRSTNAME
                UsersSortBy.LAST_NAME -> Buddies.BUDDY_LASTNAME
            }.plus(" $COLLATE_NOCASE ASC")
            context.contentResolver.load(
                Buddies.CONTENT_URI,
                arrayOf(
                    Buddies._ID,
                    Buddies.BUDDY_ID,
                    Buddies.BUDDY_NAME,
                    Buddies.BUDDY_FIRSTNAME,
                    Buddies.BUDDY_LASTNAME,
                    Buddies.AVATAR_URL,
                    Buddies.PLAY_NICKNAME,
                    Buddies.UPDATED
                ),
                "${Buddies.BUDDY_ID}!=? AND ${Buddies.BUDDY_FLAG}=1",
                arrayOf(Authenticator.getUserId(context)),
                sortOrder
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += UserEntity(
                            internalId = it.getLong(0),
                            id = it.getInt(1),
                            userName = it.getStringOrNull(2).orEmpty(),
                            firstName = it.getStringOrNull(3).orEmpty(),
                            lastName = it.getStringOrNull(4).orEmpty(),
                            avatarUrlRaw = it.getStringOrNull(5).orEmpty(),
                            playNickname = it.getStringOrNull(6).orEmpty(),
                            updatedTimestamp = it.getLongOrNull(7) ?: 0L
                        )
                    } while (it.moveToNext())
                }
            }
            results
        }

    suspend fun saveUser(user: UserEntity, updateTime: Long = System.currentTimeMillis()): UserEntity =
        withContext(Dispatchers.IO) {
            if (user.userName.isNotBlank()) {
                val values = contentValuesOf(
                    Buddies.UPDATED to updateTime,
                    Buddies.UPDATED_LIST to updateTime
                )
                val oldSyncHashCode =
                    context.contentResolver.queryInt(Buddies.buildBuddyUri(user.userName), Buddies.SYNC_HASH_CODE)
                val newSyncHashCode = user.generateSyncHashCode()
                if (oldSyncHashCode != newSyncHashCode) {
                    values.put(Buddies.BUDDY_ID, user.id)
                    values.put(Buddies.BUDDY_NAME, user.userName)
                    values.put(Buddies.BUDDY_FIRSTNAME, user.firstName)
                    values.put(Buddies.BUDDY_LASTNAME, user.lastName)
                    values.put(Buddies.AVATAR_URL, user.avatarUrl)
                    values.put(Buddies.SYNC_HASH_CODE, newSyncHashCode)
                }
                val internalId = upsert(values, user.userName, user.id)
                user.copy(internalId = internalId, updatedTimestamp = updateTime)
            } else user
        }

    fun saveUser(user: User?, updateTime: Long = System.currentTimeMillis()) {
        if (user != null && !user.name.isNullOrBlank()) {
            val values = contentValuesOf(
                Buddies.UPDATED to updateTime,
                Buddies.UPDATED_LIST to updateTime
            )
            val oldSyncHashCode =
                context.contentResolver.queryInt(Buddies.buildBuddyUri(user.name), Buddies.SYNC_HASH_CODE)
            val newSyncHashCode = generateSyncHashCode(user)
            if (oldSyncHashCode != newSyncHashCode) {
                values.put(Buddies.BUDDY_ID, user.id)
                values.put(Buddies.BUDDY_NAME, user.name)
                values.put(Buddies.BUDDY_FIRSTNAME, user.firstName)
                values.put(Buddies.BUDDY_LASTNAME, user.lastName)
                values.put(Buddies.AVATAR_URL, user.avatarUrl)
                values.put(Buddies.SYNC_HASH_CODE, newSyncHashCode)
            }
            upsert(values, user.name, user.id)
        }
    }

    suspend fun saveBuddy(buddy: BriefBuddyEntity) = withContext(Dispatchers.IO) {
        if (buddy.id != INVALID_ID && buddy.userName.isNotBlank()) {
            val values = contentValuesOf(
                Buddies.BUDDY_ID to buddy.id,
                Buddies.BUDDY_NAME to buddy.userName,
                Buddies.BUDDY_FLAG to true,
                Buddies.UPDATED_LIST to buddy.updatedTimestamp
            )
            upsert(values, buddy.userName, buddy.id)
        }
    }

    fun saveUser(
        userId: Int,
        username: String,
        isBuddy: Boolean = true,
        updateTime: Long = System.currentTimeMillis()
    ) {
        if (userId != INVALID_ID && username.isNotBlank()) {
            val values = contentValuesOf(
                Buddies.BUDDY_ID to userId,
                Buddies.BUDDY_NAME to username,
                Buddies.BUDDY_FLAG to if (isBuddy) 1 else 0,
                Buddies.UPDATED_LIST to updateTime
            )
            upsert(values, username, userId)
        } else {
            Timber.i("Un-savable buddy %s (%d)", username, userId)
        }
    }

    fun upsert(values: ContentValues, username: String, userId: Int? = null): Long {
        val resolver = context.contentResolver
        val uri = Buddies.buildBuddyUri(username)
        val internalId = resolver.queryLong(uri, Buddies._ID, INVALID_ID.toLong())
        return if (internalId != INVALID_ID.toLong()) {
            values.remove(Buddies.BUDDY_NAME)
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d buddy rows at %s", count, uri)
            maybeDeleteAvatar(values, uri)
            internalId
        } else {
            values.put(Buddies.BUDDY_NAME, username)
            userId?.let { values.put(Buddies.BUDDY_ID, it) }
            val insertedUri = resolver.insert(Buddies.CONTENT_URI, values)
            Timber.d("Inserted buddy at %s", insertedUri)
            insertedUri?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
        }
    }

    private fun generateSyncHashCode(buddy: User): Int {
        return ("${buddy.firstName}\n${buddy.lastName}\n${buddy.avatarUrl}\n").hashCode()
    }

    private fun maybeDeleteAvatar(values: ContentValues, uri: Uri) {
        if (!values.containsKey(Buddies.AVATAR_URL)) {
            // nothing to do - no avatar
            return
        }

        val newAvatarUrl: String = values.getAsString(Buddies.AVATAR_URL) ?: ""
        val oldAvatarUrl = context.contentResolver.queryString(uri, Buddies.AVATAR_URL)
        if (newAvatarUrl == oldAvatarUrl) {
            // nothing to do - avatar hasn't changed
            return
        }

        val avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl)
        if (!avatarFileName.isNullOrBlank()) {
            context.contentResolver.delete(Avatars.buildUri(avatarFileName), null, null)
        }
    }

    // TODO - convert to suspend function
    fun deleteUsersAsOf(updateTimestamp: Long): Int {
        return context.contentResolver.delete(
            Buddies.CONTENT_URI,
            "${Buddies.UPDATED_LIST}<?",
            arrayOf(updateTimestamp.toString())
        )
    }
}
