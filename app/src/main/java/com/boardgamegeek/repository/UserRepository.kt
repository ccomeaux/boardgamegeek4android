package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
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
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.User.Companion.applySort
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.pref.setBuddiesTimestamp
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.work.SyncUsersWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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

    suspend fun loadAllUsers(): List<User> = withContext(Dispatchers.IO) {
        userDao.loadUsers().map { it.mapToModel() }
    }

    fun loadUserFlow(username: String): Flow<User?> {
        return userDao.loadUserFlow(username).map { it?.mapToModel() }
    }

    suspend fun loadUsers(sortBy: User.SortType = User.SortType.USERNAME): List<User> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { userDao.loadUsers() }
            .map { it.mapToModel() }
            .applySort(sortBy)
    }

    fun loadUsersFlow(sortBy: User.SortType = User.SortType.USERNAME): Flow<List<User>> {
        return userDao.loadUsersFlow()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    suspend fun loadBuddies(sortBy: User.SortType = User.SortType.USERNAME): List<User> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO) { userDao.loadBuddies() }
            .map { it.mapToModel() }
            .applySort(sortBy)
    }

    fun loadBuddiesFlow(sortBy: User.SortType = User.SortType.USERNAME): Flow<List<User>> {
        return userDao.loadBuddiesFlow()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    suspend fun refresh(username: String, isSelf: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext null
        val timestamp = System.currentTimeMillis()
        val result = safeApiCall(context) { api.user(username) }
        if (result.isSuccess) {
            result.getOrNull()?.mapForUpsert(timestamp)?.let { user ->
                upsertUser(user)
                if (isSelf && username == Authenticator.getAccount(context)?.name) {
                    userDao.loadUser(username)?.let {
                        updateSelf(it.mapToModel())
                    }
                }
            }
        }
        result.exceptionOrNull()?.localizedMessage
    }

    suspend fun refreshCollection(username: String, status: CollectionStatus): List<CollectionItem> =
        withContext(Dispatchers.IO) {
            val response = api.collection(
                username,
                BggService.createCollectionOptionsMap(brief = true, status = status)
            )
            response.items?.map { it.mapToCollectionItem() }.orEmpty()
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
                    Timber.d("Downloaded ${buddies.size} buddies")
                    var savedCount = 0
                    buddies
                        .map { it.mapForBuddyUpsert(timestamp) }
                        .filter { it.username.isNotBlank() }
                        .forEach {
                            userDao.upsert(it)
                            savedCount++
                        }
                    Timber.d("Saved $savedCount buddies")

                    val deletedCount = userDao.deleteBuddiesAsOf(timestamp)
                    Timber.d("Deleted $deletedCount buddies")

                    Timber.i("Saved $savedCount buddies; pruned $deletedCount users who are no longer buddies")
                    syncPrefs.setBuddiesTimestamp(timestamp)
                }
            }
        }
        result.exceptionOrNull()?.localizedMessage
    }

    private suspend fun upsertUser(user: UserForUpsert) = withContext(Dispatchers.IO) {
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

    suspend fun updateNickName(username: String, nickName: String) = withContext(Dispatchers.IO) {
        if (username.isNotBlank()) userDao.updateNickname(username, nickName)
    }

    fun updateSelf(user: User?) {
        if (!user?.username.isNullOrEmpty()) FirebaseCrashlytics.getInstance().setUserId(user?.username.hashCode().toString())
        prefs[AccountPreferences.KEY_USERNAME] = user?.username.orEmpty()
        prefs[AccountPreferences.KEY_FULL_NAME] = user?.fullName.orEmpty()
        prefs[AccountPreferences.KEY_AVATAR_URL] = user?.avatarUrl.orEmpty()
    }

    suspend fun resetUsers() {
        deleteUsers()
        SyncUsersWorker.requestSync(context)
    }

    suspend fun deleteUsers() = withContext(Dispatchers.IO) {
        syncPrefs.clearBuddyListTimestamps()
        userDao.deleteAll().also { Timber.i("Deleted $it users") }
        imageDao.deleteAvatars().also { Timber.i("Deleted $it user avatars") }
    }

    suspend fun validateUsername(username: String): Boolean {
        return if (username.isBlank()) false
        else {
            val result = safeApiCall(context) { api.user(username) }
            result.getOrNull()?.name == username
        }
    }
}
