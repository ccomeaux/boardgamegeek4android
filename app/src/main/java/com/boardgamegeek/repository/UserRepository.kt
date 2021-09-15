package com.boardgamegeek.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.CollectionResponse
import com.boardgamegeek.io.model.User
import com.boardgamegeek.livedata.NetworkLoader
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.CollectionItemMapper
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.pref.setBuddiesTimestamp
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UserRepository(val application: BggApplication) {
    private val userDao = UserDao(application)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    fun loadUser(username: String): LiveData<RefreshableResource<UserEntity>> {
        return object : RefreshableResourceLoader<UserEntity, User>(application) {
            override fun loadFromDatabase(): LiveData<UserEntity> {
                return userDao.loadUserAsLiveData(username)
            }

            override fun shouldRefresh(data: UserEntity?): Boolean {
                return data == null ||
                        data.id == 0 ||
                        data.id == BggContract.INVALID_ID ||
                        data.updatedTimestamp.isOlderThan(1, TimeUnit.DAYS)
            }

            override val typeDescriptionResId = R.string.title_buddy

            override fun createCall(page: Int): Call<User> {
                return Adapter.createForXml().user(username)
            }

            override fun saveCallResult(result: User) {
                userDao.saveUser(result)
            }
        }.asLiveData()
    }

    fun loadCollection(username: String, status: String): LiveData<RefreshableResource<List<CollectionItemEntity>>> {
        return object : NetworkLoader<List<CollectionItemEntity>, CollectionResponse>(application) {
            override fun createCall(): Call<CollectionResponse> {
                val options = mapOf(
                        status to "1",
                        BggService.COLLECTION_QUERY_KEY_BRIEF to "1"
                )
                return Adapter.createForXml().collection(username, options)
            }

            override fun parseResult(result: CollectionResponse): List<CollectionItemEntity> {
                val items = mutableListOf<CollectionItemEntity>()
                val mapper = CollectionItemMapper()
                result.items.forEach {
                    items += mapper.map(it).first
                }
                return items
            }

            override val typeDescriptionResId = R.string.title_collection
        }.asLiveData()
    }

    fun loadBuddies(sortBy: UserDao.UsersSortBy = UserDao.UsersSortBy.USERNAME): LiveData<RefreshableResource<List<UserEntity>>> {
        val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }

        return object : RefreshableResourceLoader<List<UserEntity>, User>(application) {
            private var timestamp = 0L
            private var accountName: String? = null

            override fun loadFromDatabase(): LiveData<List<UserEntity>> {
                return userDao.loadBuddiesAsLiveData(sortBy)
            }

            override fun shouldRefresh(data: List<UserEntity>?): Boolean {
                if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] != true) return false
                accountName = Authenticator.getAccount(application)?.name
                if (accountName == null) return false
                if (data == null) return true
                val lastCompleteSync = syncPrefs.getBuddiesTimestamp()
                return lastCompleteSync.isOlderThan(1, TimeUnit.HOURS)
            }

            override val typeDescriptionResId = R.string.title_buddies

            override fun createCall(page: Int): Call<User> {
                timestamp = System.currentTimeMillis()
                return Adapter.createForXml().user(accountName, 1, page)
            }

            override fun saveCallResult(result: User) {
                userDao.saveUser(result.id, result.name, false)
                var upsertedCount = 0
                (result.buddies?.buddies ?: emptyList()).forEach {
                    upsertedCount += userDao.saveUser(it.id.toIntOrNull()
                            ?: BggContract.INVALID_ID, it.name, updateTime = timestamp)
                }
                Timber.d("Upserted $upsertedCount users")
            }

            override fun onRefreshSucceeded() {
                val deletedCount = userDao.deleteUsersAsOf(timestamp)
                Timber.d("Deleted $deletedCount users")
                syncPrefs.setBuddiesTimestamp(timestamp)
            }
        }.asLiveData()
    }

    fun updateNickName(username: String, nickName: String) {
        if (username.isBlank()) return
        application.appExecutors.diskIO.execute {
            userDao.updateNickName(username, nickName)
        }
    }
}