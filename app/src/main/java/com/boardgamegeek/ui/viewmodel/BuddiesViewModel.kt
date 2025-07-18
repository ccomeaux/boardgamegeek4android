package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
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
        if (syncPrefs.getBuddiesTimestamp().isOlderThan(1.days))
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

    val syncBuddies: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_BUDDIES)

    fun enableSyncing() {
        prefs[PREFERENCES_KEY_SYNC_BUDDIES] = true
        refresh()
    }

    val buddiesByHeader = _sort.switchMap {
        userRepository.loadBuddiesFlow(it).distinctUntilChanged().asLiveData().map { list ->
            list.groupBy { person -> getSectionHeader(person) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true &&
                    isRefreshing.compareAndSet(false, true)
                ) {
                    userRepository.refreshBuddies()?.let { errorMessage ->
                        _error.setMessage(errorMessage)
                    }

                    val maxBuddiesToRefresh = 10
                    val allBuddies = userRepository.loadBuddies().sortedBy { it.updatedTimestamp }
                    val (newBuddies, existingBuddies) = allBuddies.partition { it.updatedTimestamp == 0L }

                    if (newBuddies.isNotEmpty()) {
                        val n = newBuddies.size.coerceAtMost(maxBuddiesToRefresh)
                        Timber.i("Found ${newBuddies.size} buddies that haven't been updated; updating $n of them")
                        refreshUsers(newBuddies.take(n).map { it.username })?.let {
                            _error.setMessage(it)
                        }
                    }

                    val existingBuddiesToRefresh = maxBuddiesToRefresh - newBuddies.size
                    if (existingBuddiesToRefresh > 0) {
                        Timber.i("Syncing the stalest $existingBuddiesToRefresh buddies")
                        refreshUsers(existingBuddies.take(existingBuddiesToRefresh).map { it.username })?.let {
                            _error.setMessage(it)
                        }
                    }
                }
            } finally {
                _refreshing.value = false
                isRefreshing.set(false)
            }
        }
    }

    private suspend fun refreshUsers(usernames: List<String>): String? {
        val delay = 1_500L
        usernames.forEach { username ->
            Timber.i("About to refresh user $username")
            delay(delay)
            try {
                userRepository.refresh(username)
            } catch (e: Exception) {
                return e.toString()
            }
        }
        return null
    }

    fun sort(sortType: User.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    private fun getSectionHeader(user: User?): String {
        return when (sort.value) {
            User.SortType.FIRST_NAME -> user?.firstName.firstChar()
            User.SortType.LAST_NAME -> user?.lastName.firstChar()
            User.SortType.USERNAME -> user?.username.firstChar()
            null -> DEFAULT_HEADER
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}

