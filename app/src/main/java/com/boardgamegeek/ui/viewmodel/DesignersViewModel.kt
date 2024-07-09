package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.model.Person
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.DesignerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class DesignersViewModel @Inject constructor(
    application: Application,
    private val designerRepository: DesignerRepository,
) : AndroidViewModel(application) {
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

    val designers = _sort.switchMap {
        designerRepository.loadDesignersFlow(it).distinctUntilChanged().asLiveData()
    }

    private fun refreshMissingImages() {
        viewModelScope.launch {
            designerRepository.refreshMissingImages()
        }
    }

    private fun calculateStats() {
        viewModelScope.launch {
            val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                designerRepository.calculateStats(_progress)
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

    fun getSectionHeader(designer: Person?): String {
        return when(sort.value) {
            Person.SortType.NAME -> if (designer?.name == "(Uncredited)") DEFAULT_HEADER else designer?.name.firstChar()
            Person.SortType.ITEM_COUNT -> (designer?.itemCount ?: 0).orderOfMagnitude()
            Person.SortType.WHITMORE_SCORE -> (designer?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> DEFAULT_HEADER
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "-"
    }
}
