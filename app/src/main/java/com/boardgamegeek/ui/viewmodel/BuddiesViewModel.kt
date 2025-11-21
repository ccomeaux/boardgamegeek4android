package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.repository.UserRepository
import java.util.concurrent.TimeUnit

class BuddiesViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    private var refreshTimestamp = 0L
    private val userRepository = UserRepository(getApplication())

    private val _sort = MutableLiveData<BuddiesSort>()
    val sort: LiveData<BuddiesSort>
        get() = _sort

    init {
        refreshTimestamp = System.currentTimeMillis()
        sort(SortType.USERNAME)
    }

    val buddies: LiveData<RefreshableResource<List<UserEntity>>> = sort.switchMap() {
        userRepository.loadBuddies(it.sortBy)
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.USERNAME -> BuddiesSortByUsername()
            SortType.FIRST_NAME -> BuddiesSortByFirstName()
            SortType.LAST_NAME -> BuddiesSortByLastName()
        }
    }

    fun getSectionHeader(user: UserEntity?): String {
        return sort.value?.getSectionHeader(user) ?: ""
    }

    fun refresh(): Boolean {
        return if (refreshTimestamp.isOlderThan(5, TimeUnit.MINUTES)) {
            refreshTimestamp = System.currentTimeMillis()
            _sort.value = sort.value
            true
        } else {
            false
        }
    }
}

sealed class BuddiesSort {
    abstract val sortType: BuddiesViewModel.SortType
    abstract val sortBy: UserDao.UsersSortBy
    abstract fun getSectionHeader(user: UserEntity?): String
}

class BuddiesSortByUsername : BuddiesSort() {
    override val sortType = BuddiesViewModel.SortType.USERNAME
    override val sortBy = UserDao.UsersSortBy.USERNAME
    override fun getSectionHeader(user: UserEntity?): String {
        return user?.userName.firstChar()
    }
}

class BuddiesSortByFirstName : BuddiesSort() {
    override val sortType = BuddiesViewModel.SortType.FIRST_NAME
    override val sortBy = UserDao.UsersSortBy.FIRST_NAME
    override fun getSectionHeader(user: UserEntity?): String {
        return user?.firstName.firstChar()
    }
}

class BuddiesSortByLastName : BuddiesSort() {
    override val sortType = BuddiesViewModel.SortType.LAST_NAME
    override val sortBy = UserDao.UsersSortBy.LAST_NAME
    override fun getSectionHeader(user: UserEntity?): String {
        return user?.lastName.firstChar()
    }
}
