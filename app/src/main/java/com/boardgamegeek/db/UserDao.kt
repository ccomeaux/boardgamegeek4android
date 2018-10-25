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
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import timber.log.Timber

class UserDao(private val context: BggApplication) {
    fun loadBuddiesAsLiveData(): LiveData<List<UserEntity>> {
        return RegisteredLiveData(context, BggContract.Buddies.CONTENT_URI, true) {
            return@RegisteredLiveData loadBuddies()
        }
    }

    private fun loadBuddies(): List<UserEntity> {
        val results = arrayListOf<UserEntity>()
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
                arrayOf(Authenticator.getUserId(context))
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

    fun saveBuddy(userId: Int, username: String, isBuddy: Boolean = true, updateTime: Long = System.currentTimeMillis()): Int {
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
}