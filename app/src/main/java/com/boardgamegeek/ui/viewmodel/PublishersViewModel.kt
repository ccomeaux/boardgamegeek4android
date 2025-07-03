package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Company
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

    private val _statsCalculationProgress = MutableLiveData<Float>()
    val statsCalculationProgress: LiveData<Float>
        get() = _statsCalculationProgress

    private var isCalculating = AtomicBoolean()

    init {
        val initialSort = if (application.preferences().isStatusSetToSync(CollectionStatus.Rated))
            Company.SortType.WhitmoreScore
        else
            Company.SortType.ItemCount
        sort(initialSort)
        refreshMissingThumbnails()
        viewModelScope.launch {
            val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours)) {
                calculateStats()
            }
        }
    }

    val publishersByHeader = _sort.switchMap {
        publisherRepository.loadPublishersFlow(it).distinctUntilChanged().asLiveData().map { list ->
            list.groupBy { company -> getSectionHeader(company) }
        }
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

    fun calculateStats() {
        viewModelScope.launch {
            if (isCalculating.compareAndSet(false, true)) {
                publisherRepository.calculateStats(_statsCalculationProgress)
                isCalculating.set(false)
            }
        }
    }

    fun sort(sortType: Company.SortType) {
        if (_sort.value != sortType) _sort.value = sortType
    }

    private fun getSectionHeader(publisher: Company?): String {
        return when (sort.value) {
            Company.SortType.Name -> if (publisher?.name == "(Uncredited)") DEFAULT_HEADER else publisher?.name.firstChar()
            Company.SortType.ItemCount -> (publisher?.itemCount ?: 0).orderOfMagnitude()
            Company.SortType.WhitmoreScore -> (publisher?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> DEFAULT_HEADER
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}
