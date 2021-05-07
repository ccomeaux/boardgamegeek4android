package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.repository.UserRepository
import java.util.concurrent.TimeUnit
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.pref.setBuddiesTimestamp
import java.lang.Exception

class BuddiesViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    private val userRepository = UserRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }
    private var isRefreshing = false

    private val _sort = MutableLiveData<BuddiesSort>()
    val sort: LiveData<BuddiesSort>
        get() = _sort

    init {
        sort(SortType.USERNAME)
    }

    val buddies: LiveData<RefreshableResource<List<UserEntity>>> = sort.switchMap {
        liveData {
            try {
                emit(RefreshableResource.refreshing(null))
                val buddies = userRepository.loadBuddies(it.sortBy)
                if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true) {
                    val lastCompleteSync = syncPrefs.getBuddiesTimestamp()
                    if (lastCompleteSync.isOlderThan(1, TimeUnit.DAYS) && !isRefreshing) {
                        isRefreshing = true
                        emit(RefreshableResource.refreshing(buddies))
                        val timestamp = System.currentTimeMillis()
                        userRepository.refreshBuddies(timestamp)
                        syncPrefs.setBuddiesTimestamp(timestamp)
                        val refreshedBuddies = userRepository.loadBuddies(it.sortBy)
                        emit(RefreshableResource.success(refreshedBuddies))
                        isRefreshing = false
                    } else {
                        emit(RefreshableResource.success(buddies))
                    }
                } else {
                    emit(RefreshableResource.success(buddies))
                }
            } catch (e: Exception) {
                emit(RefreshableResource.error<List<UserEntity>>(e, application))
            }
        }
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.USERNAME -> BuddiesSort.ByUsername()
            SortType.FIRST_NAME -> BuddiesSort.ByFirstName()
            SortType.LAST_NAME -> BuddiesSort.ByLastName()
        }
    }

    fun getSectionHeader(user: UserEntity?): String? {
        return sort.value?.getSectionHeader(user)
    }

    fun refresh(): Boolean {
        _sort.value = sort.value
        return !isRefreshing
    }

    sealed class BuddiesSort {
        abstract val sortType: SortType
        abstract val sortBy: UserDao.UsersSortBy
        abstract fun getSectionHeader(user: UserEntity?): String

        class ByUsername : BuddiesSort() {
            override val sortType = SortType.USERNAME
            override val sortBy = UserDao.UsersSortBy.USERNAME
            override fun getSectionHeader(user: UserEntity?): String {
                return user?.userName.firstChar()
            }
        }

        class ByFirstName : BuddiesSort() {
            override val sortType = SortType.FIRST_NAME
            override val sortBy = UserDao.UsersSortBy.FIRST_NAME
            override fun getSectionHeader(user: UserEntity?): String {
                return user?.firstName.firstChar()
            }
        }

        class ByLastName : BuddiesSort() {
            override val sortType = SortType.LAST_NAME
            override val sortBy = UserDao.UsersSortBy.LAST_NAME
            override fun getSectionHeader(user: UserEntity?): String {
                return user?.lastName.firstChar()
            }
        }
    }
}

