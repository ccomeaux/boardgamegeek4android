package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.model.User
import com.boardgamegeek.provider.BggContract
import retrofit2.Call
import timber.log.Timber

class UserRepository(val application: BggApplication) {
    private val userDao = UserDao(application)

    fun loadBuddies(): LiveData<RefreshableResource<List<UserEntity>>> {
        return object : RefreshableResourceLoader<List<UserEntity>, User>(application) {
            private var timestamp = 0L

            override fun loadFromDatabase(): LiveData<List<UserEntity>> {
                return userDao.loadBuddiesAsLiveData()
            }

            override fun shouldRefresh(data: List<UserEntity>?): Boolean {
                return true
            }

            override val typeDescriptionResId = R.string.title_buddies

            override fun createCall(page: Int): Call<User>? {
                timestamp = System.currentTimeMillis()
                val account = Authenticator.getAccount(application) ?: return null
                return Adapter.createForXml().user(account.name, 1, page)
            }

            override fun saveCallResult(result: User) {
                userDao.saveBuddy(result.id, result.name, false)
                var upsertedCount = 0
                (result.buddies?.buddies ?: emptyList()).forEach {
                    upsertedCount += userDao.saveBuddy(it.id.toIntOrNull()
                            ?: BggContract.INVALID_ID, it.name, updateTime = timestamp)
                }
                Timber.d("Upserted $upsertedCount users")
                val deletedCount = userDao.deleteUsersAsOf(timestamp)
                Timber.d("Deleted $deletedCount users")
            }
        }.asLiveData()
    }
}