package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.model.Company
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.PublisherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class PublishersViewModel @Inject constructor(
    application: Application,
    private val publisherRepository: PublisherRepository,
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
            SortType.ITEM_COUNT
        sort(initialSort)
        refreshMissingThumbnails()
        calculateStats()
    }

    val publishers = _sort.switchMap {
        liveData {
            sort.value?.let {
                val sort = when(it) {
                    SortType.NAME -> PublisherRepository.SortType.NAME
                    SortType.ITEM_COUNT -> PublisherRepository.SortType.ITEM_COUNT
                    SortType.WHITMORE_SCORE -> PublisherRepository.SortType.WHITMORE_SCORE
                }
                emitSource(publisherRepository.loadPublishersAsLiveData(sort).distinctUntilChanged())
            }
        }
    }

    private fun refreshMissingThumbnails() {
        viewModelScope.launch {
            publisherRepository.refreshMissingThumbnails()
        }
    }

    private fun calculateStats() {
        viewModelScope.launch {
            val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                publisherRepository.calculateWhitmoreScores(_progress)
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

    fun getSectionHeader(publisher: Company?): String {
        return when(sort.value) {
            SortType.NAME -> if (publisher?.name == "(Uncredited)") "-" else publisher?.name.firstChar()
            SortType.ITEM_COUNT -> (publisher?.itemCount ?: 0).orderOfMagnitude()
            SortType.WHITMORE_SCORE -> (publisher?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> "-"
        }
    }
}
