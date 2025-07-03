package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Person
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
    private val _sort = MutableLiveData<Person.SortType>()
    val sort: LiveData<Person.SortType>
        get() = _sort

    private val _imageProgress = MutableLiveData<Float>()
    val imageProgress: LiveData<Float>
        get() = _imageProgress

    private val _statsCalculationProgress = MutableLiveData<Float>()
    val statsCalculationProgress: LiveData<Float>
        get() = _statsCalculationProgress

    private var isCalculatingStats = AtomicBoolean()

    init {
        val initialSort = if (application.preferences().isStatusSetToSync(CollectionStatus.Rated))
            Person.SortType.WhitmoreScore
        else
            Person.SortType.ItemCount
        sort(initialSort)
        refreshMissingImages()

        val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS, 0L] ?: 0L
        if (lastCalculation.isOlderThan(1.hours)) {
            calculateStats()
        }
    }

    val artistsByHeader = _sort.switchMap {
        artistRepository.loadArtistsFlow(it).distinctUntilChanged().asLiveData().map { list ->
            list.groupBy { person -> getSectionHeader(person) }
        }
    }

    private fun refreshMissingImages() {
        viewModelScope.launch {
            artistRepository.refreshMissingImages(progress = _imageProgress)
        }
    }

    fun calculateStats() {
        viewModelScope.launch {
            if (isCalculatingStats.compareAndSet(false, true)) {
                artistRepository.calculateStats(_statsCalculationProgress)
                isCalculatingStats.set(false)
            }
        }
    }

    fun sort(sortType: Person.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    private fun getSectionHeader(artist: Person?): String {
        return when (sort.value) {
            Person.SortType.Name -> if (artist?.name == "(Uncredited)") DEFAULT_HEADER else artist?.name.firstChar()
            Person.SortType.ItemCount -> (artist?.itemCount ?: 0).orderOfMagnitude()
            Person.SortType.WhitmoreScore -> (artist?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> DEFAULT_HEADER
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}
