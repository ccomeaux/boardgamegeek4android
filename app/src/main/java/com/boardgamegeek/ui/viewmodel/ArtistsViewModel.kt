package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.model.Person
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _sort = MutableLiveData<SortType>()
    val sort: LiveData<SortType>
        get() = _sort

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    private var isCalculating = AtomicBoolean()

    init {
        val initialSort = if (application.preferences().isStatusSetToSync(COLLECTION_STATUS_RATED))
            SortType.WHITMORE_SCORE
        else
            SortType.NAME
        sort(initialSort)
        calculateStats()
    }

    val artists = _sort.switchMap {
        liveData {
            sort.value?.let {
                val sort = when(it) {
                    SortType.NAME -> ArtistRepository.SortType.NAME
                    SortType.ITEM_COUNT -> ArtistRepository.SortType.ITEM_COUNT
                    SortType.WHITMORE_SCORE -> ArtistRepository.SortType.WHITMORE_SCORE
                }
                emitSource(artistRepository.loadArtistsAsLiveData(sort).distinctUntilChanged())
            }
        }
    }

    private fun calculateStats() {
        viewModelScope.launch {
            val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                artistRepository.calculateWhitmoreScores(_progress)
                isCalculating.set(false)
            }
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    fun getSectionHeader(artist: Person?): String {
        return when(sort.value) {
            SortType.NAME -> if (artist?.name == "(Uncredited)") "-" else artist?.name.firstChar()
            SortType.ITEM_COUNT -> (artist?.itemCount ?: 0).orderOfMagnitude()
            SortType.WHITMORE_SCORE -> (artist?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> "-"
        }
    }
}
