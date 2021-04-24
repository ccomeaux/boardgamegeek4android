package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.db.DesignerDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.DesignerRepository
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DesignersViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val designerRepository = DesignerRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }

    private val _sort = MutableLiveData<DesignersSort>()
    val sort: LiveData<DesignersSort>
        get() = _sort

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    private var isCalculating = false

    init {
        val initialSort = if (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED))
            SortType.WHITMORE_SCORE
        else
            SortType.ITEM_COUNT
        sort(initialSort)
    }

    val designers = _sort.switchMap {
        liveData {
            val designers = designerRepository.loadDesigners(it.sortBy)
            emit(designers)
            val lastCalculation = prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS, 0L] ?: 0L
            if (!isCalculating && lastCalculation.isOlderThan(1, TimeUnit.HOURS)) {
                isCalculating = true
                val job = viewModelScope.launch { designerRepository.calculateWhitmoreScores(designers, _progress) }
                job.invokeOnCompletion {
                    refresh()
                    isCalculating = false
                }
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

    fun getSectionHeader(designer: PersonEntity?): String {
        return _sort.value?.getSectionHeader(designer) ?: ""
    }

    sealed class DesignersSort {
        abstract val sortType: SortType
        abstract val sortBy: DesignerDao.SortType
        abstract fun getSectionHeader(designer: PersonEntity?): String

        class ByName : DesignersSort() {
            override val sortType = SortType.NAME
            override val sortBy = DesignerDao.SortType.NAME
            override fun getSectionHeader(designer: PersonEntity?): String {
                return if (designer?.name == "(Uncredited)") "-"
                else designer?.name.firstChar()
            }
        }

        class ByItemCount : DesignersSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = DesignerDao.SortType.ITEM_COUNT
            override fun getSectionHeader(designer: PersonEntity?): String {
                return (designer?.itemCount ?: 0).orderOfMagnitude()
            }
        }

        class ByWhitmoreScore : DesignersSort() {
            override val sortType = SortType.WHITMORE_SCORE
            override val sortBy = DesignerDao.SortType.WHITMORE_SCORE
            override fun getSectionHeader(designer: PersonEntity?): String {
                return (designer?.whitmoreScore ?: 0).orderOfMagnitude()
            }
        }
    }
}
