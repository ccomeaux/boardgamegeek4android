package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.model.Person
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    application: Application,
    private val artistRepository: ArtistRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val _sort = MutableLiveData<Person.SortType>()
    val sort: LiveData<Person.SortType>
        get() = _sort

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    private var isCalculating = AtomicBoolean()

    init {
        val initialSort = if (application.preferences().isStatusSetToSync(COLLECTION_STATUS_RATED))
            Person.SortType.WHITMORE_SCORE
        else
            Person.SortType.ITEM_COUNT
        sort(initialSort)
        refreshMissingImages()
        calculateStats()
    }

    val artists = _sort.switchMap {
        artistRepository.loadArtistsFlow(it).distinctUntilChanged().asLiveData()
    }

    private fun refreshMissingImages() {
        viewModelScope.launch {
            artistRepository.refreshMissingImages()
        }
    }

    private fun calculateStats() {
        viewModelScope.launch {
            val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                artistRepository.calculateStats(_progress)
                isCalculating.set(false)
            }
        }
    }

    fun sort(sortType: Person.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun reload() {
        _sort.value?.let { _sort.value = it }
    }

    fun getSectionHeader(artist: Person?): String {
        return when(sort.value) {
            Person.SortType.NAME -> if (artist?.name == "(Uncredited)") DEFAULT_HEADER else artist?.name.firstChar()
            Person.SortType.ITEM_COUNT -> (artist?.itemCount ?: 0).orderOfMagnitude()
            Person.SortType.WHITMORE_SCORE -> (artist?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> DEFAULT_HEADER
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}
