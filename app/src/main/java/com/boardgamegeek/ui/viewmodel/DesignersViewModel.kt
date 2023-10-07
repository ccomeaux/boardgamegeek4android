package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.Person
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.DesignerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class DesignersViewModel @Inject constructor(
    application: Application,
    designerRepository: DesignerRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val _sort = MutableLiveData<DesignersSort>()
    val sort: LiveData<DesignersSort>
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
    }

    val designers = _sort.switchMap {
        liveData {
            val designers = designerRepository.loadDesigners(it.sortBy)
            emit(designers)
            val lastCalculation = application.preferences()[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS, 0L] ?: 0L
            if (lastCalculation.isOlderThan(1.hours) &&
                isCalculating.compareAndSet(false, true)
            ) {
                designerRepository.calculateWhitmoreScores(designers, _progress)
                emit(designerRepository.loadDesigners(it.sortBy))
                isCalculating.set(false)
            }
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value?.sortType != sortType) {
            _sort.value = when (sortType) {
                SortType.NAME -> DesignersSort.ByName()
                SortType.ITEM_COUNT -> DesignersSort.ByItemCount()
                SortType.WHITMORE_SCORE -> DesignersSort.ByWhitmoreScore()
            }
        }
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    fun getSectionHeader(designer: Person?): String {
        return _sort.value?.getSectionHeader(designer).orEmpty()
    }

    sealed class DesignersSort {
        abstract val sortType: SortType
        abstract val sortBy: DesignerDao.SortType
        abstract fun getSectionHeader(designer: Person?): String

        class ByName : DesignersSort() {
            override val sortType = SortType.NAME
            override val sortBy = DesignerDao.SortType.NAME
            override fun getSectionHeader(designer: Person?): String {
                return if (designer?.name == "(Uncredited)") "-"
                else designer?.name.firstChar()
            }
        }

        class ByItemCount : DesignersSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = DesignerDao.SortType.ITEM_COUNT
            override fun getSectionHeader(designer: Person?): String {
                return (designer?.itemCount ?: 0).orderOfMagnitude()
            }
        }

        class ByWhitmoreScore : DesignersSort() {
            override val sortType = SortType.WHITMORE_SCORE
            override val sortBy = DesignerDao.SortType.WHITMORE_SCORE
            override fun getSectionHeader(designer: Person?): String {
                return (designer?.whitmoreScore ?: 0).orderOfMagnitude()
            }
        }
    }
}
