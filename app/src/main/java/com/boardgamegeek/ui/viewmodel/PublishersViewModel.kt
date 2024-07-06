package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.model.Company
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.PublisherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class PublishersViewModel @Inject constructor(
    application: Application,
    private val publisherRepository: PublisherRepository,
) : AndroidViewModel(application) {
    private val _sort = MutableLiveData<Company.SortType>()
    val sort: LiveData<Company.SortType>
        get() = _sort

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    private var isCalculating = AtomicBoolean()

    init {
        val initialSort = if (application.preferences().isStatusSetToSync(COLLECTION_STATUS_RATED))
            Company.SortType.WHITMORE_SCORE
        else
            Company.SortType.ITEM_COUNT
        sort(initialSort)
        refreshMissingThumbnails()
        calculateStats()
    }

    val publishers = _sort.switchMap {
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            sort.value?.let {
                emitSource(publisherRepository.loadPublishersFlow(it).distinctUntilChanged().asLiveData())
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
                publisherRepository.calculateStats(_progress)
                isCalculating.set(false)
            }
        }
    }

    fun sort(sortType: Company.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    fun reload() {
        _sort.value?.let { _sort.value = it }
    }

    fun getSectionHeader(publisher: Company?): String {
        return when(sort.value) {
            Company.SortType.NAME -> if (publisher?.name == "(Uncredited)") defaultHeader else publisher?.name.firstChar()
            Company.SortType.ITEM_COUNT -> (publisher?.itemCount ?: 0).orderOfMagnitude()
            Company.SortType.WHITMORE_SCORE -> (publisher?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> defaultHeader
        }
    }

    companion object {
        const val defaultHeader = "-"
    }
}
