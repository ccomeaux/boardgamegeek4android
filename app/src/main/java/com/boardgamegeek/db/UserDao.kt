package com.boardgamegeek.db

import android.content.ContentValues
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.model.User
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import timber.log.Timber

class UserDao(private val context: BggApplication) {
    enum class UsersSortBy {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    fun loadBuddiesAsLiveData(sortBy: UsersSortBy = UsersSortBy.USERNAME): LiveData<List<UserEntity>> {
        return RegisteredLiveData(context, BggContract.Buddies.CONTENT_URI, true) {
            return@RegisteredLiveData loadBuddies(sortBy)
        }
    }

    fun loadUserAsLiveData(username: String): LiveData<UserEntity> {
        return RegisteredLiveData(context, BggContract.Buddies.buildBuddyUri(username), true) {
            return@RegisteredLiveData loadUser(username)
        }
    }

    private fun loadUser(username: String): UserEntity? {
        return context.contentResolver.load(
                BggContract.Buddies.buildBuddyUri(username),
                arrayOf(
                        BggContract.Buddies._ID,
                        BggContract.Buddies.BUDDY_ID,
                        BggContract.Buddies.BUDDY_NAME,
                        BggContract.Buddies.BUDDY_FIRSTNAME,
                        BggContract.Buddies.BUDDY_LASTNAME,
                        BggContract.Buddies.AVATAR_URL,
                        BggContract.Buddies.PLAY_NICKNAME,
                        BggContract.Buddies.UPDATED
                )
        )?.use {
            if (it.moveToFirst()) {
                UserEntity(
                        it.getLong(BggContract.Buddies._ID),
                        it.getInt(BggContract.Buddies.BUDDY_ID),
                        it.getStringOrEmpty(BggContract.Buddies.BUDDY_NAME),
                        it.getStringOrEmpty(BggContract.Buddies.BUDDY_FIRSTNAME),
                        it.getStringOrEmpty(BggContract.Buddies.BUDDY_LASTNAME),
                        it.getStringOrEmpty(BggContract.Buddies.AVATAR_URL),
                        it.getStringOrEmpty(BggContract.Buddies.PLAY_NICKNAME),
                        it.getLongOrZero(BggContract.Buddies.UPDATED)
                )
            } else null
        }
    }

    private fun loadBuddies(sortBy: UsersSortBy = UsersSortBy.USERNAME): List<UserEntity> {
        val results = arrayListOf<UserEntity>()
        val sortOrder = when (sortBy) {
            UsersSortBy.USERNAME -> BggContract.Buddies.BUDDY_NAME
            UsersSortBy.FIRST_NAME -> BggContract.Buddies.BUDDY_FIRSTNAME
            UsersSortBy.LAST_NAME -> BggContract.Buddies.BUDDY_LASTNAME
        }.plus(" ${BggContract.COLLATE_NOCASE} ASC")
        context.contentResolver.load(
                BggContract.Buddies.CONTENT_URI,
                arrayOf(
                        BggContract.Buddies._ID,
                        BggContract.Buddies.BUDDY_ID,
                        BggContract.Buddies.BUDDY_NAME,
                        BggContract.Buddies.BUDDY_FIRSTNAME,
                        BggContract.Buddies.BUDDY_LASTNAME,
                        BggContract.Buddies.AVATAR_URL,
                        BggContract.Buddies.PLAY_NICKNAME,
                        BggContract.Buddies.UPDATED
                ),
                "${BggContract.Buddies.BUDDY_ID}!=? AND ${BggContract.Buddies.BUDDY_FLAG}=1",
                arrayOf(Authenticator.getUserId(context)),
                sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += UserEntity(
                            it.getLong(BggContract.Buddies._ID),
                            it.getInt(BggContract.Buddies.BUDDY_ID),
                            it.getStringOrEmpty(BggContract.Buddies.BUDDY_NAME),
                            it.getStringOrEmpty(BggContract.Buddies.BUDDY_FIRSTNAME),
                            it.getStringOrEmpty(BggContract.Buddies.BUDDY_LASTNAME),
                            it.getStringOrEmpty(BggContract.Buddies.AVATAR_URL),
                            it.getStringOrEmpty(BggContract.Buddies.PLAY_NICKNAME),
                            it.getLongOrZero(BggContract.Buddies.UPDATED)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun saveUser(user: User?, updateTime: Long = System.currentTimeMillis()): Int {
        if (user != null && !user.name.isNullOrBlank()) {
            val values = contentValuesOf(
                    BggContract.Buddies.UPDATED to updateTime,
                    BggContract.Buddies.UPDATED_LIST to updateTime
            )
            val oldSyncHashCode = context.contentResolver.queryInt(BggContract.Buddies.buildBuddyUri(user.name), BggContract.Buddies.SYNC_HASH_CODE)
            val newSyncHashCode = generateSyncHashCode(user)
            if (oldSyncHashCode != newSyncHashCode) {
                values.put(BggContract.Buddies.BUDDY_ID, user.id)
                values.put(BggContract.Buddies.BUDDY_NAME, user.name)
                values.put(BggContract.Buddies.BUDDY_FIRSTNAME, user.firstName)
                values.put(BggContract.Buddies.BUDDY_LASTNAME, user.lastName)
                values.put(BggContract.Buddies.AVATAR_URL, user.avatarUrl)
                values.put(BggContract.Buddies.SYNC_HASH_CODE, newSyncHashCode)
            }
            return upsert(values, user.name, user.id)
        }
        return 0
    }

    fun saveUser(userId: Int, username: String, isBuddy: Boolean = true, updateTime: Long = System.currentTimeMillis()): Int {
        if (userId != BggContract.INVALID_ID && username.isNotBlank()) {
            val values = contentValuesOf(
                    BggContract.Buddies.BUDDY_ID to userId,
                    BggContract.Buddies.BUDDY_NAME to username,
                    BggContract.Buddies.BUDDY_FLAG to if (isBuddy) 1 else 0,
                    BggContract.Buddies.UPDATED_LIST to updateTime
            )
            return upsert(values, username, userId)
        } else {
            Timber.i("Un-savable buddy %s (%d)", username, userId)
        }
        return 0
    }

    private fun upsert(values: ContentValues, username: String, userId: Int): Int {
        val resolver = context.contentResolver
        val uri = BggContract.Buddies.buildBuddyUri(username)
        return if (resolver.rowExists(uri)) {
            values.remove(BggContract.Buddies.BUDDY_NAME)
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d buddy rows at %s", count, uri)
            maybeDeleteAvatar(values, uri)
            count
        } else {
            values.put(BggContract.Buddies.BUDDY_NAME, username)
            values.put(BggContract.Buddies.BUDDY_ID, userId)
            val insertedUri = resolver.insert(BggContract.Buddies.CONTENT_URI, values)
            Timber.d("Inserted buddy at %s", insertedUri)
            1
        }
    }

    private fun generateSyncHashCode(buddy: User): Int {
        return ("${buddy.firstName}\n${buddy.lastName}\n${buddy.avatarUrl}\n").hashCode()
    }

    private fun maybeDeleteAvatar(values: ContentValues, uri: Uri) {
        if (!values.containsKey(BggContract.Buddies.AVATAR_URL)) {
            // nothing to do - no avatar
            return
        }

        val newAvatarUrl: String = values.getAsString(BggContract.Buddies.AVATAR_URL) ?: ""
        val oldAvatarUrl = context.contentResolver.queryString(uri, BggContract.Buddies.AVATAR_URL)
        if (newAvatarUrl == oldAvatarUrl) {
            // nothing to do - avatar hasn't changed
            return
        }

        val avatarFileName = FileUtils.getFileNameFromUrl(oldAvatarUrl)
        if (!avatarFileName.isNullOrBlank()) {
            context.contentResolver.delete(BggContract.Avatars.buildUri(avatarFileName), null, null)
        }
    }

    fun deleteUsersAsOf(updateTimestamp: Long): Int {
        return context.contentResolver.delete(
                BggContract.Buddies.CONTENT_URI,
                "${BggContract.Buddies.UPDATED_LIST}<?",
                arrayOf(updateTimestamp.toString()))
    }

    fun updateNickName(username: String, nickName: String) {
        context.contentResolver.update(
                BggContract.Buddies.buildBuddyUri(username),
                contentValuesOf(BggContract.Buddies.PLAY_NICKNAME to nickName),
                null,
                null
        )
    }
}