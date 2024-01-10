package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.ImageDao
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.User
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.*
import com.boardgamegeek.mappers.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.pref.setBuddiesTimestamp
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.work.SyncUsersWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserRepository(
    val context: Context,
    private val api: BggService,
    private val userDao: UserDao,
) {
    private val imageDao = ImageDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context) }

    enum class UsersSortBy {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    suspend fun loadUser(username: String): User? = withContext(Dispatchers.IO) {
        if (username.isBlank()) null
        else userDao.loadUser(username)?.mapToModel()
    }

    fun loadUserAsLiveData(username: String): LiveData<User?> {
        return if (username.isBlank()) MutableLiveData(null)
        else userDao.loadUserAsLiveData(username).map { it.mapToModel() }
    }

    suspend fun refresh(username: String): String? = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext null
        val timestamp = System.currentTimeMillis()
        val result = safeApiCall(context) { api.user(username) }
        if (result.isSuccess) {
            result.getOrNull()?.mapForUpsert(timestamp)?.let { user ->
                upsertUser(user)
            }
        }
        result.exceptionOrNull()?.localizedMessage
    }

    suspend fun refreshCollection(username: String, status: String): List<CollectionItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<CollectionItem>()
            val response = api.collection(
                username, mapOf(
                    status to "1",
                    BggService.COLLECTION_QUERY_KEY_BRIEF to "1"
                )
            )
            response.items?.forEach {
                items += it.mapToCollectionItem()
            }
            items
        }

    suspend fun loadBuddies(sortBy: UsersSortBy = UsersSortBy.USERNAME): List<User> = withContext(Dispatchers.IO) {
        userDao.loadUsers()
            .map { it.mapToModel() }
            .filterAndSortBuddies(sortBy)
    }

    suspend fun loadBuddiesAsLiveData(sortBy: UsersSortBy = UsersSortBy.USERNAME): LiveData<List<User>> = withContext(Dispatchers.Default) {
        userDao.loadUsersAsLiveData().map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.filterAndSortBuddies(sortBy)
        }
    }

    private fun List<User>.filterAndSortBuddies(sortBy: UsersSortBy): List<User> {
        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        return this.filter { it.isBuddy && it.username != username }
            .sortedWith(
                compareBy(
                    String.CASE_INSENSITIVE_ORDER
                ) {
                    when (sortBy) {
                        UsersSortBy.FIRST_NAME -> it.firstName
                        UsersSortBy.LAST_NAME -> it.lastName
                        UsersSortBy.USERNAME -> it.username
                    }
                }
            )
    }

    suspend fun loadAllUsers(): List<User> = withContext(Dispatchers.IO) {
        userDao.loadUsers().map { it.mapToModel() }
    }

    suspend fun refreshBuddies(): String? = withContext(Dispatchers.IO) {
        val accountName = Authenticator.getAccount(context)?.name
        if (accountName.isNullOrBlank()) return@withContext context.getString(R.string.msg_sync_unauthed)

        val timestamp = System.currentTimeMillis()
        val result = safeApiCall(context) { api.user(accountName, 1, 1) }
        if (result.isSuccess) {
            result.getOrNull()?.let { user ->
                upsertUser(user.mapForUpsert(timestamp)) // update the current user
                user.buddies?.buddies?.let { buddies ->
                    var savedCount = 0
                    buddies
                        .map { it.mapForBuddyUpsert(timestamp) }
                        .filter { it.username.isNotBlank() }
                        .forEach {
                            userDao.upsert(it)
                            savedCount++
                        }
                    Timber.d("Downloaded ${buddies.size} buddies, saved $savedCount buddies")

                    val deletedCount = userDao.deleteBuddiesAsOf(timestamp)
                    Timber.d("Deleted $deletedCount buddies")

                    Timber.i("Saved $savedCount buddies; pruned $deletedCount users who are no longer buddies")
                    syncPrefs.setBuddiesTimestamp(timestamp)
                }
            }
        }
        result.exceptionOrNull()?.localizedMessage
    }

    private suspend fun upsertUser(user: UserForUpsert) {
        val storedUser = userDao.loadUser(user.username)
        if (storedUser == null) {
            userDao.insert(user)
        } else if (storedUser.syncHashCode == user.syncHashCode) {
            userDao.updateTimestamp(user.username, user.updatedDetailTimestamp.time)
        } else {
            userDao.update(user)
            if (user.avatarUrl != storedUser.avatarUrl) {
                val avatarFileName = FileUtils.getFileNameFromUrl(storedUser.avatarUrl)
                imageDao.deleteFile(avatarFileName, BggContract.PATH_AVATARS)
            }
        }
    }

    suspend fun validateUsername(username: String): Boolean = withContext(Dispatchers.IO) {
        if (username.isBlank()) false
        else {
            val result = safeApiCall(context) { api.user(username) }
            result.getOrNull()?.name == username
        }
    }

    suspend fun updateNickName(username: String, nickName: String) = withContext(Dispatchers.IO) {
        if (username.isNotBlank()) userDao.updateNickname(username, nickName)
    }

    fun updateSelf(user: User?) {
        if (!user?.username.isNullOrEmpty()) FirebaseCrashlytics.getInstance().setUserId(user?.username.hashCode().toString())
        prefs[AccountPreferences.KEY_USERNAME] = user?.username.orEmpty()
        prefs[AccountPreferences.KEY_FULL_NAME] = user?.fullName.orEmpty()
        prefs[AccountPreferences.KEY_AVATAR_URL] = user?.avatarUrl.orEmpty()
    }

    suspend fun deleteUsers(): Int {
        syncPrefs.clearBuddyListTimestamps()
        val count = userDao.deleteAll()
        imageDao.deleteAvatars()
        Timber.i("Removed %d users", count)
        return count
    }

    suspend fun resetUsers() {
        deleteUsers()
        SyncUsersWorker.requestSync(context)
    }
}
