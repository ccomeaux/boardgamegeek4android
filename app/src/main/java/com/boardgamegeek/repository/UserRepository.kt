package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.ImageDao
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.User
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.work.SyncUsersWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserRepository(
    val context: Context,
    private val api: BggService,
) {
    private val userDao = UserDao(context)
    private val imageDao = ImageDao(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(context) }

    suspend fun load(username: String): User? = withContext(Dispatchers.IO) {
        userDao.loadUser(username)?.mapToModel()
    }

    suspend fun refresh(username: String) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.user(username)
        val user = response.mapForUpsert(timestamp)
        userDao.saveUser(user)
    }

    suspend fun refreshCollection(username: String, status: String): List<CollectionItemEntity> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<CollectionItemEntity>()
            val response = api.collection(
                username, mapOf(
                    status to "1",
                    BggService.COLLECTION_QUERY_KEY_BRIEF to "1"
                )
            )
            response.items?.forEach {
                items += it.mapToCollectionItemEntity()
            }
            items
        }

    suspend fun loadBuddies(sortBy: UserDao.UsersSortBy = UserDao.UsersSortBy.USERNAME): List<User> = withContext(Dispatchers.IO) {
        userDao.loadUsers(sortBy, buddiesOnly = true).map { it.mapToModel() }
    }

    suspend fun loadAllUsers(): List<User> = withContext(Dispatchers.IO) {
        userDao.loadUsers(buddiesOnly = false).map { it.mapToModel() }
    }

    suspend fun refreshBuddies(timestamp: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val accountName = Authenticator.getAccount(context)?.name
        if (accountName.isNullOrBlank()) return@withContext 0 to 0

        val response = api.user(accountName, 1, 1)

        userDao.saveUser(response.mapForUpsert(timestamp))

        val downloadedCount = response.buddies?.buddies?.size ?: 0
        var savedCount = 0
        response.buddies?.buddies.orEmpty()
            .map { it.mapForBuddyUpsert(timestamp) }
            .filter { it.buddyId != INVALID_ID && it.userName.isNotBlank() }
            .forEach {
                userDao.saveBuddy(it)
                savedCount++
            }
        Timber.d("Downloaded $downloadedCount users, saved $savedCount users")

        val deletedCount = userDao.deleteBuddiesAsOf(timestamp)
        Timber.d("Deleted $deletedCount users")

        savedCount to deletedCount
    }

    suspend fun validateUsername(username: String): Boolean = withContext(Dispatchers.IO) {
        val response = api.user(username)
        response.name == username
    }

    suspend fun updateNickName(username: String, nickName: String) {
        userDao.updateNickname(username, nickName)
    }

    fun updateSelf(user: User?) {
        Authenticator.putUserId(context, user?.id ?: INVALID_ID)
        if (!user?.userName.isNullOrEmpty()) FirebaseCrashlytics.getInstance().setUserId(user?.userName.hashCode().toString())
        prefs[AccountPreferences.KEY_USERNAME] = user?.userName.orEmpty()
        prefs[AccountPreferences.KEY_FULL_NAME] = user?.fullName.orEmpty()
        prefs[AccountPreferences.KEY_AVATAR_URL] = user?.avatarUrl.orEmpty()
    }

    suspend fun deleteUsers(): Int {
        syncPrefs.clearBuddyListTimestamps()
        val count = userDao.deleteUsers()
        imageDao.deleteAvatars()
        Timber.i("Removed %d users", count)
        return count
    }

    suspend fun resetUsers() {
        deleteUsers()
        SyncUsersWorker.requestSync(context)
    }

    suspend fun updateColors(username: String, colors: List<Pair<Int, String>>) = userDao.updateColors(username, colors)
}
