package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    application: Application,
    artistRepository: ArtistRepository
) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val prefs: SharedPreferences by lazy { application.preferences() }

    private val _sort = MutableLiveData<ArtistsSort>()
    val sort: LiveData<ArtistsSort>
        get() = _sort

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    private var isCalculating = AtomicBoolean()

    init {
        val initialSort = if (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED))
            SortType.WHITMORE_SCORE
        else
            SortType.ITEM_COUNT
        sort(initialSort)
    }

    val artists = _sort.switchMap {
        liveData {
            val artists = artistRepository.loadArtists(it.sortBy)
            emit(artists)
            val lastCalculation = prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1, TimeUnit.HOURS) &&
                isCalculating.compareAndSet(false, true)
            ) {
                artistRepository.calculateWhitmoreScores(artists, _progress)
                emit(artistRepository.loadArtists(it.sortBy))
                isCalculating.set(false)
            }
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value?.sortType != sortType) {
            _sort.value = when (sortType) {
                SortType.NAME -> ArtistsSort.ByName()
                SortType.ITEM_COUNT -> ArtistsSort.ByItemCount()
                SortType.WHITMORE_SCORE -> ArtistsSort.ByWhitmoreScore()
            }
        }
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    fun getSectionHeader(artist: PersonEntity?): String {
        return _sort.value?.getSectionHeader(artist).orEmpty()
    }

    sealed class ArtistsSort {
        abstract val sortType: SortType
        abstract val sortBy: ArtistDao.SortType
        abstract fun getSectionHeader(artist: PersonEntity?): String

        class ByName : ArtistsSort() {
            override val sortType = SortType.NAME
            override val sortBy = ArtistDao.SortType.NAME
            override fun getSectionHeader(artist: PersonEntity?): String {
                return if (artist?.name == "(Uncredited)") "-"
                else artist?.name.firstChar()
            }
        }

        class ByItemCount : ArtistsSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = ArtistDao.SortType.ITEM_COUNT
            override fun getSectionHeader(artist: PersonEntity?): String {
                return (artist?.itemCount ?: 0).orderOfMagnitude()
            }
        }

        class ByWhitmoreScore : ArtistsSort() {
            override val sortType = SortType.WHITMORE_SCORE
            override val sortBy = ArtistDao.SortType.WHITMORE_SCORE
            override fun getSectionHeader(artist: PersonEntity?): String {
                return (artist?.whitmoreScore ?: 0).orderOfMagnitude()
            }
        }
    }
}
