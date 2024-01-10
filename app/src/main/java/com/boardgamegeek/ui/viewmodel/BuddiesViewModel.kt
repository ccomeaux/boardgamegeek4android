package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.model.User
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.extensions.*
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class BuddiesViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }
    private var isRefreshing = AtomicBoolean()

    private val _sort = MutableLiveData<SortType>()
    val sort: LiveData<SortType>
        get() = _sort

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _error = MutableLiveData<String>()
    val error: LiveData<String>
        get() = _error

    init {
        sort(SortType.USERNAME)
        refresh()
    }

    val buddies: LiveData<List<User>> = sort.switchMap {
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                val sortBy = when (it) {
                    SortType.FIRST_NAME -> UserRepository.UsersSortBy.FIRST_NAME
                    SortType.LAST_NAME -> UserRepository.UsersSortBy.LAST_NAME
                    SortType.USERNAME -> UserRepository.UsersSortBy.USERNAME
                }
                emitSource(userRepository.loadBuddiesAsLiveData(sortBy))
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true &&
                    syncPrefs.getBuddiesTimestamp().isOlderThan(1.days) &&
                    isRefreshing.compareAndSet(false, true)
                ) {
                    userRepository.refreshBuddies()?.let { errorMessage ->
                        _error.value = errorMessage
                    }
                }
            } finally {
                _refreshing.value = false
                isRefreshing.set(false)
            }
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun getSectionHeader(user: User?): String {
        return when (sort.value) {
            SortType.FIRST_NAME -> user?.firstName.firstChar()
            SortType.LAST_NAME -> user?.lastName.firstChar()
            SortType.USERNAME -> user?.username.firstChar()
            null -> "-"
        }
    }
}

