package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.model.Play
import com.boardgamegeek.repository.PlayRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())

    val syncPlays: LiveData<Boolean> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS, defaultValue = false)

    enum class FilterType {
        ALL, DIRTY, PENDING
    }

    enum class SortType {
        DATE, LOCATION, GAME, LENGTH
    }

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean>
        get() = _isRefreshing

    private val _plays = MediatorLiveData<List<Play>>()

    private val _filterType = MutableLiveData<FilterType>()
    val filterType: LiveData<FilterType>
        get() = _filterType

    private val _sortType = MutableLiveData<SortType>()
    val sortType: LiveData<SortType>
        get() = _sortType

    private val allPlays: LiveData<List<Play>> = playRepository.loadPlaysFlow().distinctUntilChanged().asLiveData()

    init {
        _plays.addSource(allPlays) { list ->
            filterAndSortPlays(list, sortType.value, filterType.value)
        }
        _plays.addSource(sortType) {
            filterAndSortPlays(allPlays.value, it, filterType.value)
        }
        _plays.addSource(filterType) {
            filterAndSortPlays(allPlays.value, sortType.value, it)
        }
        setFilter(FilterType.ALL)
        setSort(SortType.DATE)
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }

    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val plays = _plays.map { list ->
        list.groupBy { play ->
            when (sortType.value) {
                SortType.DATE -> {
                    if (play.dateInMillis == Play.UNKNOWN_DATE)
                        DEFAULT_HEADER
                    else
                        dateFormat.format(play.dateInMillis)!!
                }
                SortType.LOCATION -> {
                    play.location.ifBlank { DEFAULT_HEADER }
                }
                SortType.GAME -> {
                    play.gameName.ifBlank { DEFAULT_HEADER }
                }
                SortType.LENGTH -> {
                    val minutes = play.length
                    when {
                        minutes == 0 -> getApplication<BggApplication>().getString(R.string.no_length)
                        minutes >= 120 -> "${(minutes / 60)}+ ${getApplication<BggApplication>().getString(R.string.hours_abbr)}"
                        minutes >= 60 -> "${(minutes / 10 * 10)}+ ${getApplication<BggApplication>().getString(R.string.minutes_abbr)}"
                        minutes >= 30 -> "${(minutes / 5 * 5)}+ ${getApplication<BggApplication>().getString(R.string.minutes_abbr)}"
                        else -> "$minutes ${getApplication<BggApplication>().getString(R.string.minutes_abbr)}"
                    }
                }
                null -> DEFAULT_HEADER
            }

        }
    }

    private fun filterAndSortPlays(
        list: List<Play>?,
        sortType: SortType?,
        filterType: FilterType?,
    ) {
        if (list == null) return
        val filteredList = when (filterType) {
            FilterType.ALL -> list.filter { it.deleteTimestamp == 0L }
            FilterType.DIRTY -> list.filter { it.dirtyTimestamp > 0L }
            FilterType.PENDING -> list.filter { it.updateTimestamp > 0L || it.deleteTimestamp > 0L }
            null -> list
        }
        val sortedList = when (sortType) {
            SortType.DATE -> filteredList.sortedByDescending { it.dateInMillis }
            SortType.LOCATION -> filteredList.sortedBy { it.location }
            SortType.GAME -> filteredList.sortedBy { it.gameName }
            SortType.LENGTH -> filteredList.sortedByDescending { it.length }
            null -> filteredList.sortedByDescending { it.dateInMillis }
        }
        _plays.postValue(sortedList)
    }

    fun setFilter(type: FilterType) {
        firebaseAnalytics.logEvent("Filter") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
            bundle.putString("FilterBy", type.toString())
        }
        if (_filterType.value != type) _filterType.value = type
    }

    fun setSort(type: SortType) {
        firebaseAnalytics.logEvent("Sort") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
            param("SortBy", type.toString())
        }
        if (sortType.value != type) _sortType.value = type
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                if (syncPlays.value == true && _isRefreshing.value != true) {
                    _isRefreshing.postValue(true)
                    playRepository.refreshRecentPlays()
                }
            } catch (e: Exception) {
                _errorMessage.postMessage(e)
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }

    fun refreshPlaysByDate(timeInMillis: Long) {
        viewModelScope.launch {
            try {
                if (syncPlays.value == true && _isRefreshing.value != true) {
                    _isRefreshing.postValue(true)
                    Timber.w(
                        "CPC refreshing ${
                            timeInMillis.formatDateTime(
                                getApplication(),
                                flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                            )
                        }"
                    )
                    playRepository.refreshPlaysForDate(timeInMillis)?.let {
                        _errorMessage.postMessage(it)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postMessage(e)
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }

    fun send(playIds: List<Long>) {
        viewModelScope.launch {
            val idsToSend = mutableListOf<Long>()
            playIds.forEach {
                if (playRepository.markAsUpdated(it))
                    idsToSend += it
            }
            playRepository.enqueueUploadRequest(idsToSend)
        }
    }

    fun delete(playIds: List<Long>) {
        viewModelScope.launch {
            val idsDeleted = mutableListOf<Long>()
            playIds.forEach {
                if (playRepository.markAsDeleted(it))
                    idsDeleted += it
            }
            playRepository.enqueueUploadRequest(idsDeleted)
        }
    }
}
