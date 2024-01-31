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
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(application.applicationContext) }
    private var isRefreshing = AtomicBoolean()

    private val _sort = MutableLiveData<User.SortType>()
    val sort: LiveData<User.SortType>
        get() = _sort

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _error = EventLiveData()
    val error: LiveData<Event<String>>
        get() = _error

    init {
        sort(User.SortType.USERNAME)
        refresh()
    }

    val buddies: LiveData<List<User>> = sort.switchMap {
        liveData {
            try {
                emitSource(userRepository.loadBuddiesFlow(it).distinctUntilChanged().asLiveData())
            } catch (e: Exception) {
                _error.setMessage(e)
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
                        _error.setMessage(errorMessage)
                    }
                }
            } finally {
                _refreshing.value = false
                isRefreshing.set(false)
            }
        }
    }

    fun sort(sortType: User.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun getSectionHeader(user: User?): String {
        return when (sort.value) {
            User.SortType.FIRST_NAME -> user?.firstName.firstChar()
            User.SortType.LAST_NAME -> user?.lastName.firstChar()
            User.SortType.USERNAME -> user?.username.firstChar()
            null -> defaultHeader
        }
    }

    companion object {
        const val defaultHeader = "-"
    }
}

