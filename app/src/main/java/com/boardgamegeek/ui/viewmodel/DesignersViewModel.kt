package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.model.Person
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.DesignerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class DesignersViewModel @Inject constructor(
    application: Application,
    private val designerRepository: DesignerRepository,
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
        calculateStats()
    }

    val designers = _sort.switchMap {
        liveData {
            sort.value?.let {
                val sort = when(it) {
                    SortType.NAME -> DesignerRepository.SortType.NAME
                    SortType.ITEM_COUNT -> DesignerRepository.SortType.ITEM_COUNT
                    SortType.WHITMORE_SCORE -> DesignerRepository.SortType.WHITMORE_SCORE
                }
                emitSource(designerRepository.loadDesignersAsLiveData(sort).distinctUntilChanged())
            }
        }
    }

    private fun calculateStats() {
        viewModelScope.launch {
            val lastCalculation = getApplication<BggApplication>().preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                designerRepository.calculateWhitmoreScores(_progress)
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

    fun getSectionHeader(designer: Person?): String {
        return when(sort.value) {
            SortType.NAME -> if (designer?.name == "(Uncredited)") "-" else designer?.name.firstChar()
            SortType.ITEM_COUNT -> (designer?.itemCount ?: 0).orderOfMagnitude()
            SortType.WHITMORE_SCORE -> (designer?.whitmoreScore ?: 0).orderOfMagnitude()
            else -> "-"
        }
    }
}
